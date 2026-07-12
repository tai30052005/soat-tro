package com.example.soattro.service;

import com.example.soattro.ai.ClauseAnalyzer;
import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractedClause;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.ai.RawFinding;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.dto.response.ChecklistItemDto;
import com.example.soattro.dto.response.ClauseDto;
import com.example.soattro.dto.response.FindingDto;
import com.example.soattro.entity.Analysis;
import com.example.soattro.entity.AnalysisStatus;
import com.example.soattro.entity.ChecklistItem;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.Finding;
import com.example.soattro.exception.BadRequestException;
import com.example.soattro.exception.ResourceNotFoundException;
import com.example.soattro.repository.AnalysisRepository;
import com.example.soattro.repository.ChecklistItemRepository;
import com.example.soattro.repository.FindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ soát hợp đồng — điều phối pipeline 3 bước (SPEC mục 4):
 *   [1] ContractExtractor  : ảnh/PDF -> điều khoản đã phân loại (Gemini Vision).
 *   [2] ClauseAnalyzer     : điều khoản -> findings rủi ro (Gemini).
 *   [3] code tất định      : GroundingVerifier -> ChecklistBuilder -> ScoreCalculator,
 *                            LawReferences gắn căn cứ luật.
 *
 * Chạy ĐỒNG BỘ (chặng 6 sẽ chuyển @Async). Privacy-by-design: bytes ảnh chỉ sống
 * trong request, không ghi đĩa/DB.
 */
@Service
public class AnalysisService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final int MAX_FILES = 10;
    private static final long MAX_TOTAL_BYTES = 12L * 1024 * 1024;

    private final ContractExtractor extractor;
    private final ClauseAnalyzer analyzer;
    private final GroundingVerifier groundingVerifier;
    private final ChecklistBuilder checklistBuilder;
    private final ScoreCalculator scoreCalculator;
    private final LawReferences lawReferences;
    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final CurrentUserService currentUserService;

    public AnalysisService(ContractExtractor extractor,
                           ClauseAnalyzer analyzer,
                           GroundingVerifier groundingVerifier,
                           ChecklistBuilder checklistBuilder,
                           ScoreCalculator scoreCalculator,
                           LawReferences lawReferences,
                           AnalysisRepository analysisRepository,
                           FindingRepository findingRepository,
                           ChecklistItemRepository checklistItemRepository,
                           CurrentUserService currentUserService) {
        this.extractor = extractor;
        this.analyzer = analyzer;
        this.groundingVerifier = groundingVerifier;
        this.checklistBuilder = checklistBuilder;
        this.scoreCalculator = scoreCalculator;
        this.lawReferences = lawReferences;
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * POST /api/analyses: validate -> pipeline 3 bước -> lưu -> trả kết quả đầy đủ.
     *
     * KHÔNG @Transactional: nếu bọc chung transaction, khi AI ném lỗi thì bản ghi
     * FAILED (ghi trong catch) cũng bị rollback -> mất dấu vết. Mỗi save commit ngay;
     * lỗi AI xảy ra TRƯỚC khi ghi finding/checklist nào nên không sợ dữ liệu ghi dở.
     */
    public AnalysisResponse create(List<MultipartFile> files) {
        List<ContractExtractor.InputFile> inputs = validateAndRead(files);

        Analysis analysis = new Analysis(currentUserService.getCurrentUser().orElse(null));
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysisRepository.save(analysis);

        try {
            // ---- Bước 1: bóc tách điều khoản ----
            ExtractionResult extraction = extractor.extract(inputs);
            if (!extraction.readable()) {
                analysis.setStatus(AnalysisStatus.FAILED);
                analysis.setErrorMessage(extraction.reason());
                analysisRepository.save(analysis);
                return AnalysisResponse.basic(analysis);
            }
            List<ExtractedClause> clauses = extraction.clauses();
            String contractText = clauses.stream()
                    .map(ExtractedClause::text)
                    .collect(Collectors.joining("\n\n"));
            analysis.setContractText(contractText);

            // ---- Bước 2: phân tích rủi ro ----
            List<RawFinding> rawFindings = analyzer.analyze(clauses);

            // ---- Bước 3a: verify grounding (loại finding bịa) ----
            List<RawFinding> grounded = groundingVerifier.verify(contractText, rawFindings);

            // ---- Bước 3b: checklist thiếu (đếm slot taxonomy trống) ----
            Set<ClauseType> presentTypes = clauses.stream()
                    .map(ExtractedClause::clauseType)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClauseType.class)));
            Map<ClauseType, Boolean> checklist = checklistBuilder.build(presentTypes);
            List<ClauseType> missingEssential = checklist.entrySet().stream()
                    .filter(e -> !e.getValue())
                    .map(Map.Entry::getKey)
                    .toList();

            // ---- Bước 3c: chấm điểm tất định ----
            ScoreResult score = scoreCalculator.calculate(grounded, missingEssential);

            // ---- Lưu findings + checklist + điểm ----
            List<Finding> savedFindings = persistFindings(analysis, grounded);
            persistChecklist(analysis, checklist);
            analysis.setSafetyScore(score.score());
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysisRepository.save(analysis);

            return new AnalysisResponse(
                    analysis.getId(), analysis.getStatus().name(), score.score(),
                    score.verdictLabel(), score.summary(),
                    contractText, null, analysis.getCreatedAt(),
                    clauses.stream().map(ClauseDto::from).toList(),
                    savedFindings.stream().map(FindingDto::from).toList(),
                    toChecklistDtos(checklist));
        } catch (RuntimeException e) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysisRepository.save(analysis);
            throw e;
        }
    }

    /** GET /api/analyses/{id} — nạp lại kết quả đã lưu (findings + checklist + điểm). */
    @Transactional(readOnly = true)
    public AnalysisResponse get(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt phân tích " + id));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            return AnalysisResponse.basic(analysis);
        }
        List<FindingDto> findings = findingRepository.findByAnalysisId(id).stream()
                .map(FindingDto::from).toList();
        List<ChecklistItemDto> checklist = checklistItemRepository.findByAnalysisId(id).stream()
                .map(ChecklistItemDto::from).toList();
        Integer sc = analysis.getSafetyScore();
        String label = sc != null ? scoreCalculator.labelFor(sc) : null;

        return new AnalysisResponse(
                analysis.getId(), analysis.getStatus().name(), sc, label, null,
                analysis.getContractText(), null, analysis.getCreatedAt(),
                List.of(), findings, checklist);
    }

    private List<Finding> persistFindings(Analysis analysis, List<RawFinding> grounded) {
        List<Finding> entities = new ArrayList<>();
        for (RawFinding f : grounded) {
            entities.add(new Finding(analysis, f.clauseType(), f.riskLevel(),
                    f.quote(), f.explanation(), f.suggestion(),
                    lawReferences.forType(f.clauseType())));   // căn cứ luật do CODE gắn
        }
        return findingRepository.saveAll(entities);
    }

    private void persistChecklist(Analysis analysis, Map<ClauseType, Boolean> checklist) {
        List<ChecklistItem> items = checklist.entrySet().stream()
                .map(e -> new ChecklistItem(analysis, e.getKey(), e.getValue()))
                .toList();
        checklistItemRepository.saveAll(items);
    }

    private List<ChecklistItemDto> toChecklistDtos(Map<ClauseType, Boolean> checklist) {
        return checklist.entrySet().stream()
                .map(e -> ChecklistItemDto.of(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ContractExtractor.InputFile> validateAndRead(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BadRequestException("Hãy chọn ít nhất 1 ảnh hoặc file PDF của hợp đồng.");
        }
        if (files.size() > MAX_FILES) {
            throw new BadRequestException("Tối đa " + MAX_FILES + " ảnh mỗi lần soát.");
        }

        long total = 0;
        for (MultipartFile f : files) {
            String type = f.getContentType();
            if (type == null || !ALLOWED_TYPES.contains(type)) {
                throw new BadRequestException(
                        "File '" + f.getOriginalFilename() + "' không được hỗ trợ — chỉ nhận ảnh JPG/PNG/WEBP hoặc PDF.");
            }
            total += f.getSize();
        }
        if (total > MAX_TOTAL_BYTES) {
            throw new BadRequestException("Tổng dung lượng vượt 12MB — hãy chụp lại với độ phân giải thấp hơn.");
        }

        try {
            List<ContractExtractor.InputFile> inputs = new ArrayList<>();
            for (MultipartFile f : files) {
                if (!f.isEmpty()) {
                    inputs.add(new ContractExtractor.InputFile(f.getContentType(), f.getBytes()));
                }
            }
            return inputs;
        } catch (IOException e) {
            throw new BadRequestException("Không đọc được file tải lên, hãy thử lại.");
        }
    }
}

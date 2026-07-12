package com.example.soattro.service;

import com.example.soattro.ai.ClauseAnalyzer;
import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractedClause;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.ai.RawFinding;
import com.example.soattro.entity.Analysis;
import com.example.soattro.entity.AnalysisStatus;
import com.example.soattro.entity.ChecklistItem;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.Finding;
import com.example.soattro.repository.AnalysisRepository;
import com.example.soattro.repository.ChecklistItemRepository;
import com.example.soattro.repository.FindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chạy pipeline soát hợp đồng trên luồng nền (chặng 6).
 *
 * Tách khỏi AnalysisService vì @Async chỉ hiệu lực khi được gọi từ BEAN KHÁC
 * (self-invocation không đi qua proxy). AnalysisService.create() lưu bản ghi
 * PROCESSING rồi gọi sang đây; pipeline 3 bước (SPEC mục 4) chạy nền:
 *   [1] ContractExtractor -> [2] ClauseAnalyzer -> [3] verify/checklist/score (code tất định).
 */
@Component
public class AnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(AnalysisProcessor.class);

    private final ContractExtractor extractor;
    private final ClauseAnalyzer analyzer;
    private final GroundingVerifier groundingVerifier;
    private final ChecklistBuilder checklistBuilder;
    private final ScoreCalculator scoreCalculator;
    private final LawReferences lawReferences;
    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final ChecklistItemRepository checklistItemRepository;

    public AnalysisProcessor(ContractExtractor extractor,
                             ClauseAnalyzer analyzer,
                             GroundingVerifier groundingVerifier,
                             ChecklistBuilder checklistBuilder,
                             ScoreCalculator scoreCalculator,
                             LawReferences lawReferences,
                             AnalysisRepository analysisRepository,
                             FindingRepository findingRepository,
                             ChecklistItemRepository checklistItemRepository) {
        this.extractor = extractor;
        this.analyzer = analyzer;
        this.groundingVerifier = groundingVerifier;
        this.checklistBuilder = checklistBuilder;
        this.scoreCalculator = scoreCalculator;
        this.lawReferences = lawReferences;
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.checklistItemRepository = checklistItemRepository;
    }

    /**
     * Xử lý nền một lượt soát đã ở trạng thái PROCESSING.
     * KHÔNG @Transactional: mỗi save commit ngay để trạng thái (kể cả FAILED)
     * luôn hiện ra khi frontend poll — không bị rollback nếu bước sau lỗi.
     */
    @Async("analysisExecutor")
    public void process(Long analysisId, List<ContractExtractor.InputFile> inputs) {
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            log.warn("Không tìm thấy lượt soát {} để xử lý nền", analysisId);
            return;
        }

        try {
            // ---- Bước 1: bóc tách điều khoản ----
            ExtractionResult extraction = extractor.extract(inputs);
            if (!extraction.readable()) {
                analysis.setStatus(AnalysisStatus.FAILED);
                analysis.setErrorMessage(extraction.reason());
                analysisRepository.save(analysis);
                return;
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

            // ---- Bước 3b: checklist thiếu ----
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
            persistFindings(analysis, grounded);
            persistChecklist(analysis, checklist);
            analysis.setSafetyScore(score.score());
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysisRepository.save(analysis);
        } catch (RuntimeException e) {
            log.error("Lỗi khi soát hợp đồng {}: {}", analysisId, e.getMessage());
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysisRepository.save(analysis);
        }
    }

    private void persistFindings(Analysis analysis, List<RawFinding> grounded) {
        List<Finding> entities = new ArrayList<>();
        for (RawFinding f : grounded) {
            entities.add(new Finding(analysis, f.clauseType(), f.riskLevel(),
                    f.quote(), f.explanation(), f.suggestion(),
                    lawReferences.forType(f.clauseType())));   // căn cứ luật do CODE gắn
        }
        findingRepository.saveAll(entities);
    }

    private void persistChecklist(Analysis analysis, Map<ClauseType, Boolean> checklist) {
        List<ChecklistItem> items = checklist.entrySet().stream()
                .map(e -> new ChecklistItem(analysis, e.getKey(), e.getValue()))
                .toList();
        checklistItemRepository.saveAll(items);
    }
}

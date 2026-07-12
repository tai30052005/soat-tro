package com.example.soattro.service;

import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.dto.response.AnalysisSummary;
import com.example.soattro.dto.response.ChecklistItemDto;
import com.example.soattro.dto.response.FindingDto;
import com.example.soattro.entity.Analysis;
import com.example.soattro.entity.AnalysisStatus;
import com.example.soattro.entity.User;
import com.example.soattro.exception.BadRequestException;
import com.example.soattro.exception.ResourceNotFoundException;
import com.example.soattro.exception.UnauthorizedException;
import com.example.soattro.repository.AnalysisRepository;
import com.example.soattro.repository.ChecklistItemRepository;
import com.example.soattro.repository.FindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Điều phối nghiệp vụ soát hợp đồng (chặng 6 — bất đồng bộ).
 *
 *  - create(): validate + đọc bytes ảnh (trong luồng request) -> lưu bản ghi PROCESSING
 *    -> giao pipeline cho AnalysisProcessor chạy NỀN -> trả về ngay.
 *  - get():    frontend poll để lấy trạng thái/kết quả.
 *  - history(): danh sách lượt soát của user đang đăng nhập.
 *
 * Privacy-by-design: bytes ảnh chỉ sống trong request + luồng nền, không ghi đĩa/DB.
 */
@Service
public class AnalysisService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final int MAX_FILES = 10;
    private static final long MAX_TOTAL_BYTES = 12L * 1024 * 1024;

    private final AnalysisProcessor processor;
    private final ScoreCalculator scoreCalculator;
    private final SampleContractProvider sampleContractProvider;
    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final CurrentUserService currentUserService;

    public AnalysisService(AnalysisProcessor processor,
                           ScoreCalculator scoreCalculator,
                           SampleContractProvider sampleContractProvider,
                           AnalysisRepository analysisRepository,
                           FindingRepository findingRepository,
                           ChecklistItemRepository checklistItemRepository,
                           CurrentUserService currentUserService) {
        this.processor = processor;
        this.scoreCalculator = scoreCalculator;
        this.sampleContractProvider = sampleContractProvider;
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.currentUserService = currentUserService;
    }

    /** GET /api/analyses/sample — kết quả demo dựng sẵn, không gọi Gemini, không đụng DB. */
    public AnalysisResponse sample() {
        return sampleContractProvider.build();
    }

    /**
     * POST /api/analyses: validate + đọc bytes -> lưu PROCESSING -> chạy nền -> trả ngay.
     * Có token thì gắn lượt soát vào user (để xem lịch sử); ẩn danh thì user = null.
     */
    public AnalysisResponse create(List<MultipartFile> files) {
        List<ContractExtractor.InputFile> inputs = validateAndRead(files);

        Analysis analysis = new Analysis(currentUserService.getCurrentUser().orElse(null));
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysisRepository.save(analysis);

        // Giao cho luồng nền — đọc bytes phải xong TRƯỚC (MultipartFile hết hiệu lực khi request kết thúc).
        processor.process(analysis.getId(), inputs);

        return AnalysisResponse.basic(analysis);   // status = PROCESSING, frontend sẽ poll
    }

    /** GET /api/analyses/{id} — poll trạng thái; xong thì nạp findings + checklist + điểm. */
    @Transactional(readOnly = true)
    public AnalysisResponse get(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt phân tích " + id));

        // Riêng tư: lượt soát ẩn danh (user=null) công khai để giữ luồng poll không cần token;
        // lượt đã gắn tài khoản thì CHỈ chủ nhân xem được (tránh đoán id tuần tự đọc hợp đồng người khác).
        // Trả 404 (không phải 403) để không lộ việc id đó có tồn tại.
        if (analysis.getUser() != null) {
            Long currentUserId = currentUserService.getCurrentUser().map(User::getId).orElse(null);
            if (!analysis.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Không tìm thấy lượt phân tích " + id);
            }
        }

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

    /** GET /api/analyses — lịch sử soát của user đang đăng nhập (bắt buộc đăng nhập). */
    @Transactional(readOnly = true)
    public List<AnalysisSummary> history() {
        User user = currentUserService.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Hãy đăng nhập để xem lịch sử soát."));

        return analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(a -> {
                    String label = a.getSafetyScore() != null
                            ? scoreCalculator.labelFor(a.getSafetyScore()) : null;
                    return AnalysisSummary.from(a, label);
                })
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

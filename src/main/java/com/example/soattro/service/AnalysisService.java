package com.example.soattro.service;

import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.dto.response.ClauseDto;
import com.example.soattro.entity.Analysis;
import com.example.soattro.entity.AnalysisStatus;
import com.example.soattro.exception.BadRequestException;
import com.example.soattro.exception.ResourceNotFoundException;
import com.example.soattro.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ soát hợp đồng — chặng 3: nhận file, gọi AI bóc tách, lưu kết quả.
 *
 * Chạy ĐỒNG BỘ (request chờ ~20-30s) — chặng 6 sẽ chuyển sang @Async + poll.
 * Privacy-by-design: bytes ảnh chỉ sống trong bộ nhớ của request này,
 * không ghi xuống đĩa, không lưu DB — xử lý xong là bỏ.
 */
@Service
public class AnalysisService {

    /** Các định dạng chấp nhận: ảnh chụp điện thoại + PDF scan. */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private static final int MAX_FILES = 10;
    /**
     * Tổng dung lượng ≤ 12MB: Gemini giới hạn request ~20MB mà base64 phình ~33%
     * (12MB file -> ~16MB base64 + prompt, còn biên an toàn).
     */
    private static final long MAX_TOTAL_BYTES = 12L * 1024 * 1024;

    private final ContractExtractor extractor;
    private final AnalysisRepository analysisRepository;
    private final CurrentUserService currentUserService;

    public AnalysisService(ContractExtractor extractor,
                           AnalysisRepository analysisRepository,
                           CurrentUserService currentUserService) {
        this.extractor = extractor;
        this.analysisRepository = analysisRepository;
        this.currentUserService = currentUserService;
    }

    /** POST /api/analyses: validate file -> gọi Gemini -> lưu bản ghi -> trả kết quả. */
    public AnalysisResponse create(List<MultipartFile> files) {
        List<ContractExtractor.InputFile> inputs = validateAndRead(files);

        // Tạo bản ghi trước khi gọi AI: nếu AI lỗi giữa chừng vẫn còn dấu vết FAILED.
        Analysis analysis = new Analysis(currentUserService.getCurrentUser().orElse(null));
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysisRepository.save(analysis);

        try {
            ExtractionResult result = extractor.extract(inputs);

            if (!result.readable()) {
                analysis.setStatus(AnalysisStatus.FAILED);
                analysis.setErrorMessage(result.reason());
                analysisRepository.save(analysis);
                return AnalysisResponse.from(analysis, List.of());
            }

            // contract_text = ghép nguyên văn các điều khoản, 2 dòng trống ngăn cách.
            // Đây là "nguồn sự thật" để chặng 4 verify grounding từng finding.
            String contractText = result.clauses().stream()
                    .map(c -> c.text())
                    .collect(Collectors.joining("\n\n"));
            analysis.setContractText(contractText);
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysisRepository.save(analysis);

            List<ClauseDto> clauses = result.clauses().stream().map(ClauseDto::from).toList();
            return AnalysisResponse.from(analysis, clauses);
        } catch (RuntimeException e) {
            // Lỗi gọi AI (quota, mạng...) -> ghi FAILED rồi ném tiếp cho GlobalExceptionHandler
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysisRepository.save(analysis);
            throw e;
        }
    }

    /** GET /api/analyses/{id}. */
    public AnalysisResponse get(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt phân tích " + id));
        return AnalysisResponse.from(analysis, List.of());
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
            List<ContractExtractor.InputFile> inputs = new java.util.ArrayList<>();
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

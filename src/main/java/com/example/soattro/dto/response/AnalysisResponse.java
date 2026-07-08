package com.example.soattro.dto.response;

import com.example.soattro.entity.Analysis;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả một lượt soát trả cho frontend.
 * clauses chỉ có ngay sau POST (chặng 3 chưa lưu điều khoản xuống DB —
 * chặng 4 sẽ chuyển thành findings có phân tích rủi ro đầy đủ).
 */
public record AnalysisResponse(
        Long id,
        String status,
        Integer safetyScore,
        String contractText,
        String errorMessage,
        LocalDateTime createdAt,
        List<ClauseDto> clauses
) {

    public static AnalysisResponse from(Analysis a, List<ClauseDto> clauses) {
        return new AnalysisResponse(
                a.getId(),
                a.getStatus().name(),
                a.getSafetyScore(),
                a.getContractText(),
                a.getErrorMessage(),
                a.getCreatedAt(),
                clauses);
    }
}

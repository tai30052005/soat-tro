package com.example.soattro.dto.response;

import com.example.soattro.entity.Analysis;

import java.time.LocalDateTime;

/**
 * Dòng tóm tắt một lượt soát cho trang lịch sử (chặng 6) — nhẹ, không kèm
 * findings/checklist (chỉ nạp chi tiết khi mở từng lượt).
 */
public record AnalysisSummary(
        Long id,
        String status,
        Integer safetyScore,
        String verdictLabel,
        LocalDateTime createdAt
) {

    public static AnalysisSummary from(Analysis a, String verdictLabel) {
        return new AnalysisSummary(
                a.getId(), a.getStatus().name(), a.getSafetyScore(),
                verdictLabel, a.getCreatedAt());
    }
}

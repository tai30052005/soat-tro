package com.example.soattro.dto.response;

import com.example.soattro.entity.Analysis;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả một lượt soát trả cho frontend (dữ liệu cho toàn bộ trang kết quả chặng 5).
 *  - clauses: điều khoản nguyên văn đã bóc (chỉ có ngay sau POST, để highlight).
 *  - findings: nhận định rủi ro từng điều khoản (RED/YELLOW/GREEN + quote + giải thích).
 *  - checklist: hợp đồng thiếu loại điều khoản thiết yếu nào.
 */
public record AnalysisResponse(
        Long id,
        String status,
        Integer safetyScore,
        String verdictLabel,
        String summary,
        String contractText,
        String errorMessage,
        LocalDateTime createdAt,
        List<ClauseDto> clauses,
        List<FindingDto> findings,
        List<ChecklistItemDto> checklist
) {

    /** Bản rút gọn khi thất bại / chưa có kết quả (không có findings, checklist). */
    public static AnalysisResponse basic(Analysis a) {
        return new AnalysisResponse(
                a.getId(), a.getStatus().name(), a.getSafetyScore(),
                null, null, a.getContractText(), a.getErrorMessage(), a.getCreatedAt(),
                List.of(), List.of(), List.of());
    }
}

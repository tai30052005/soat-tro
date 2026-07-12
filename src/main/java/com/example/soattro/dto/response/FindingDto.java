package com.example.soattro.dto.response;

import com.example.soattro.entity.Finding;

/** Nhận định một điều khoản, trả cho frontend (dùng cho highlight màu + giải thích). */
public record FindingDto(
        String clauseType,
        String label,
        String riskLevel,
        String quote,
        String explanation,
        String suggestion,
        String lawRef
) {

    public static FindingDto from(Finding f) {
        return new FindingDto(
                f.getClauseType().name(),
                f.getClauseType().getLabel(),
                f.getRiskLevel().name(),
                f.getQuote(),
                f.getExplanation(),
                f.getSuggestion(),
                f.getLawRef());
    }
}

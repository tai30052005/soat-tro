package com.example.soattro.dto.response;

import com.example.soattro.ai.ExtractedClause;

/** Điều khoản trả về cho frontend: mã + nhãn tiếng Việt + nguyên văn. */
public record ClauseDto(String clauseType, String label, String text) {

    public static ClauseDto from(ExtractedClause c) {
        return new ClauseDto(c.clauseType().name(), c.clauseType().getLabel(), c.text());
    }
}

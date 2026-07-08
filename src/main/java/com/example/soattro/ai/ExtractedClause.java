package com.example.soattro.ai;

import com.example.soattro.entity.ClauseType;

/** Một điều khoản đã bóc từ hợp đồng: nguyên văn + mã taxonomy. */
public record ExtractedClause(ClauseType clauseType, String text) {
}

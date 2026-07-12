package com.example.soattro.ai;

import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;

/**
 * Finding "thô" do Gemini sinh ở bước 2 — CHƯA qua kiểm định.
 * Chưa có lawRef: căn cứ pháp lý sẽ do CODE gắn (LawReferences), không để AI tự trích.
 */
public record RawFinding(
        ClauseType clauseType,
        RiskLevel riskLevel,
        String quote,
        String explanation,
        String suggestion
) {
}

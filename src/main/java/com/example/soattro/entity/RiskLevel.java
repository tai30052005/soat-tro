package com.example.soattro.entity;

/**
 * Mức rủi ro của một điều khoản (theo rubric, mục 3).
 * severity dùng để chọn finding NẶNG NHẤT khi một loại điều khoản có nhiều finding.
 */
public enum RiskLevel {
    GREEN(0),   // ổn, rõ ràng, đúng luật
    YELLOW(1),  // mơ hồ, cần làm rõ trước khi ký
    RED(2);     // bất lợi rõ ràng / trái quy định

    private final int severity;

    RiskLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}

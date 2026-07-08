package com.example.soattro.entity;

/**
 * Vòng đời một lượt phân tích. Chặng 3 chạy đồng bộ nên chỉ dùng
 * PROCESSING → COMPLETED/FAILED; PENDING sẽ dùng khi chuyển @Async (chặng 6).
 */
public enum AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

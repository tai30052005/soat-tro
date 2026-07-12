package com.example.soattro.entity;

/**
 * Vòng đời một lượt phân tích (chặng 6 chạy bất đồng bộ):
 * create() lưu PROCESSING rồi giao luồng nền -> COMPLETED (có điểm) hoặc FAILED (kèm lý do).
 * PENDING giữ lại làm mặc định entity phòng khi mở rộng hàng đợi sau này.
 */
public enum AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

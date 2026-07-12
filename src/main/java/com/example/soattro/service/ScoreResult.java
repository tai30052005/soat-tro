package com.example.soattro.service;

/**
 * Kết quả chấm điểm: điểm 0-100 + nhãn màu + nhận định 1 câu (cho trang kết quả).
 */
public record ScoreResult(int score, String verdictLabel, String summary) {
}

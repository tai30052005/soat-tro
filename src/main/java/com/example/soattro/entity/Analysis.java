package com.example.soattro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Một lượt soát hợp đồng, ánh xạ bảng 'analyses' (Flyway V1).
 *
 * user NULLABLE: người dùng ẩn danh vẫn soát được — đăng nhập chỉ để lưu lịch sử.
 * Privacy-by-design: KHÔNG có cột ảnh gốc, chỉ giữ text đã bóc tách.
 */
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * LAZY: chỉ truy vấn bảng users khi thật sự gọi getUser()
     * (tránh JOIN thừa mỗi lần đọc analysis).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Lưu tên enum dạng chuỗi ('PROCESSING'...) — khớp cột VARCHAR trong V1. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    /** Văn bản hợp đồng đã bóc từ ảnh/PDF (nguồn đối chiếu grounding ở chặng 4). */
    @Column(name = "contract_text")
    private String contractText;

    /** Điểm an toàn 0–100 do CODE tính (chặng 4), không phải AI chấm. */
    @Column(name = "safety_score")
    private Integer safetyScore;

    /** Lý do khi status = FAILED (vd "Ảnh không đủ rõ để đọc"). */
    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Analysis() {
    }

    public Analysis(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public String getContractText() {
        return contractText;
    }

    public void setContractText(String contractText) {
        this.contractText = contractText;
    }

    public Integer getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(Integer safetyScore) {
        this.safetyScore = safetyScore;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

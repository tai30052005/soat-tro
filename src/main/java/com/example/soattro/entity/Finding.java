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

/**
 * Nhận định về một điều khoản, ánh xạ bảng 'findings' (Flyway V1).
 * quote đã được GroundingVerifier đối chiếu khớp contract_text trước khi lưu.
 */
@Entity
@Table(name = "findings")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "clause_type", nullable = false)
    private ClauseType clauseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    /** Trích dẫn nguyên văn từ hợp đồng (bằng chứng, chống AI bịa). */
    @Column(nullable = false)
    private String quote;

    @Column(nullable = false)
    private String explanation;

    /** Gợi ý câu hỏi / cách thương lượng với chủ trọ (có thể null với GREEN). */
    private String suggestion;

    /** Căn cứ pháp lý — CODE gắn từ rubric, không phải AI sinh. */
    @Column(name = "law_ref")
    private String lawRef;

    protected Finding() {
    }

    public Finding(Analysis analysis, ClauseType clauseType, RiskLevel riskLevel,
                   String quote, String explanation, String suggestion, String lawRef) {
        this.analysis = analysis;
        this.clauseType = clauseType;
        this.riskLevel = riskLevel;
        this.quote = quote;
        this.explanation = explanation;
        this.suggestion = suggestion;
        this.lawRef = lawRef;
    }

    public Long getId() {
        return id;
    }

    public ClauseType getClauseType() {
        return clauseType;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getQuote() {
        return quote;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getLawRef() {
        return lawRef;
    }
}

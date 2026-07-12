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
 * Một dòng checklist "hợp đồng này THIẾU gì", ánh xạ bảng 'checklist_items' (V1).
 * present do CODE kết luận (đếm loại thiết yếu có xuất hiện hay không), không phải AI.
 */
@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "clause_type", nullable = false)
    private ClauseType clauseType;

    @Column(nullable = false)
    private boolean present;

    protected ChecklistItem() {
    }

    public ChecklistItem(Analysis analysis, ClauseType clauseType, boolean present) {
        this.analysis = analysis;
        this.clauseType = clauseType;
        this.present = present;
    }

    public Long getId() {
        return id;
    }

    public ClauseType getClauseType() {
        return clauseType;
    }

    public boolean isPresent() {
        return present;
    }
}

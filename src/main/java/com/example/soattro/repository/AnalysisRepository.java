package com.example.soattro.repository;

import com.example.soattro.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /** Lịch sử soát của một user, mới nhất trước (dùng ở chặng 6). */
    List<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}

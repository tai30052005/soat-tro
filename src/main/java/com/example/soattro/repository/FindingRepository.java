package com.example.soattro.repository;

import com.example.soattro.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByAnalysisId(Long analysisId);
}

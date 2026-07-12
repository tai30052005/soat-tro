package com.example.soattro.controller;

import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.dto.response.AnalysisSummary;
import com.example.soattro.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * API soát hợp đồng. KHÔNG bắt đăng nhập (SecurityConfig permitAll /api/analyses/**)
 * — có token thì service tự gắn lượt soát vào user để xem lại lịch sử.
 */
@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * POST /api/analyses — multipart, field "files" chứa 1..10 ảnh/PDF.
     * consumes MULTIPART_FORM_DATA: khai báo rõ để Swagger sinh form upload đúng.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> create(@RequestParam("files") List<MultipartFile> files) {
        AnalysisResponse response = analysisService.create(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /api/analyses/{id} — xem lại một lượt soát (frontend poll trạng thái). */
    @GetMapping("/{id}")
    public AnalysisResponse get(@PathVariable Long id) {
        return analysisService.get(id);
    }

    /** GET /api/analyses — lịch sử soát của user đang đăng nhập (ẩn danh -> 401). */
    @GetMapping
    public List<AnalysisSummary> history() {
        return analysisService.history();
    }
}

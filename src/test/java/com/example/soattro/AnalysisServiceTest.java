package com.example.soattro;

import com.example.soattro.ai.ClauseAnalyzer;
import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractedClause;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.ai.RawFinding;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import com.example.soattro.exception.BadRequestException;
import com.example.soattro.repository.AnalysisRepository;
import com.example.soattro.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Test nghiệp vụ AnalysisService trên H2, MOCK ContractExtractor
 * (không gọi Gemini thật trong test — nhanh, ổn định, không tốn quota).
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisServiceTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisRepository analysisRepository;

    @MockBean
    private ContractExtractor extractor;

    // ClauseAnalyzer cũng mock: pipeline bước 2 gọi Gemini, không chạy thật trong test.
    @MockBean
    private ClauseAnalyzer analyzer;

    private MockMultipartFile jpg(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void createHappyPathRunsFullPipeline() {
        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"),
                new ExtractedClause(ClauseType.DAT_COC, "Cọc 1 tháng tiền nhà"))));
        // Finding có quote khớp nguyên văn -> qua được grounding
        when(analyzer.analyze(anyList())).thenReturn(List.of(
                new RawFinding(ClauseType.GIA_DIEN, RiskLevel.RED, "Tiền điện 4.000đ/kWh",
                        "Giá điện cao hơn quy định", "Hỏi lại chủ trọ")));

        AnalysisResponse response = analysisService.create(List.of(jpg("hopdong.jpg")));

        assertEquals("COMPLETED", response.status());
        assertEquals(2, response.clauses().size());
        assertNotNull(response.id());
        assertNotNull(response.safetyScore());          // code đã chấm điểm
        assertNotNull(response.verdictLabel());
        assertTrue(response.contractText().contains("Tiền điện 4.000đ/kWh"));
        // Finding grounded được lưu + căn cứ luật do code gắn
        assertEquals(1, response.findings().size());
        assertEquals("RED", response.findings().get(0).riskLevel());
        assertNotNull(response.findings().get(0).lawRef());
        // Checklist gồm 14 loại thiết yếu; HOAN_COC thiếu -> present=false
        assertEquals(14, response.checklist().size());
        assertTrue(response.checklist().stream()
                .anyMatch(c -> c.clauseType().equals("HOAN_COC") && !c.present()));
        assertTrue(analysisRepository.findById(response.id()).isPresent());
    }

    @Test
    void hallucinatedFindingIsDroppedByGrounding() {
        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"))));
        // Quote KHÔNG có trong hợp đồng -> grounding phải loại, không lưu, không trừ điểm
        when(analyzer.analyze(anyList())).thenReturn(List.of(
                new RawFinding(ClauseType.DON_PHUONG_CHAM_DUT, RiskLevel.RED, "Chủ nhà đuổi bất cứ lúc nào",
                        "Điều khoản bịa", null)));

        AnalysisResponse response = analysisService.create(List.of(jpg("hopdong.jpg")));

        assertEquals("COMPLETED", response.status());
        assertTrue(response.findings().isEmpty());      // finding bịa bị loại
    }

    @Test
    void unreadableImageBecomesFailedWithReason() {
        when(extractor.extract(anyList())).thenReturn(
                new ExtractionResult(false, "Ảnh bị mờ, không đọc được chữ", List.of()));

        AnalysisResponse response = analysisService.create(List.of(jpg("mo.jpg")));

        assertEquals("FAILED", response.status());
        assertEquals("Ảnh bị mờ, không đọc được chữ", response.errorMessage());
    }

    @Test
    void rejectsEmptyFileList() {
        assertThrows(BadRequestException.class, () -> analysisService.create(List.of()));
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile exe = new MockMultipartFile(
                "files", "virus.exe", "application/octet-stream", new byte[]{1});

        assertThrows(BadRequestException.class, () -> analysisService.create(List.of(exe)));
    }

    @Test
    void rejectsWhenTotalSizeOver12Mb() {
        MockMultipartFile big = new MockMultipartFile(
                "files", "to.jpg", "image/jpeg", new byte[13 * 1024 * 1024]);

        assertThrows(BadRequestException.class, () -> analysisService.create(List.of(big)));
    }

    @Test
    void aiFailureIsRecordedAsFailed() {
        when(extractor.extract(anyList()))
                .thenThrow(new BadRequestException("Đã hết lượt AI miễn phí hôm nay — hãy thử lại sau."));

        assertThrows(BadRequestException.class, () -> analysisService.create(List.of(jpg("a.jpg"))));

        // Vẫn còn dấu vết FAILED trong DB để trang lịch sử hiển thị tử tế
        assertTrue(analysisRepository.findAll().stream()
                .anyMatch(a -> a.getStatus().name().equals("FAILED")));
    }
}

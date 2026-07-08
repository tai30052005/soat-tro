package com.example.soattro;

import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractedClause;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.entity.ClauseType;
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

    private MockMultipartFile jpg(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void createHappyPathSavesCompletedAnalysis() {
        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"),
                new ExtractedClause(ClauseType.DAT_COC, "Cọc 1 tháng tiền nhà"))));

        AnalysisResponse response = analysisService.create(List.of(jpg("hopdong.jpg")));

        assertEquals("COMPLETED", response.status());
        assertEquals(2, response.clauses().size());
        assertNotNull(response.id());
        // contract_text ghép nguyên văn -> nguồn đối chiếu grounding chặng 4
        assertTrue(response.contractText().contains("Tiền điện 4.000đ/kWh"));
        assertTrue(analysisRepository.findById(response.id()).isPresent());
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

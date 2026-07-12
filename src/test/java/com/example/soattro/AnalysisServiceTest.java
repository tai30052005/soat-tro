package com.example.soattro;

import com.example.soattro.ai.ClauseAnalyzer;
import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractedClause;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.ai.RawFinding;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.dto.response.AnalysisSummary;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import com.example.soattro.exception.BadRequestException;
import com.example.soattro.exception.UnauthorizedException;
import com.example.soattro.repository.AnalysisRepository;
import com.example.soattro.repository.UserRepository;
import com.example.soattro.service.AnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Test nghiệp vụ AnalysisService trên H2. MOCK ContractExtractor + ClauseAnalyzer
 * (không gọi Gemini thật). Dùng SyncExecutorTestConfig để pipeline @Async chạy
 * đồng bộ ngay trong create() -> kết quả tất định, không phải chờ poll.
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisServiceTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ContractExtractor extractor;

    @MockBean
    private ClauseAnalyzer analyzer;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private MockMultipartFile jpg(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    private void loginAs(String email) {
        UserDetails principal = User.withUsername(email).password("x")
                .authorities(AuthorityUtils.NO_AUTHORITIES).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void createReturnsProcessingThenPipelineCompletes() {
        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"),
                new ExtractedClause(ClauseType.DAT_COC, "Cọc 1 tháng tiền nhà"))));
        when(analyzer.analyze(anyList())).thenReturn(List.of(
                new RawFinding(ClauseType.GIA_DIEN, RiskLevel.RED, "Tiền điện 4.000đ/kWh",
                        "Giá điện cao hơn quy định", "Hỏi lại chủ trọ")));

        // create() trả về ngay với trạng thái PROCESSING
        AnalysisResponse created = analysisService.create(List.of(jpg("hopdong.jpg")));
        assertEquals("PROCESSING", created.status());
        assertNotNull(created.id());

        // Với sync executor, pipeline đã chạy xong -> poll GET thấy COMPLETED
        AnalysisResponse done = analysisService.get(created.id());
        assertEquals("COMPLETED", done.status());
        assertNotNull(done.safetyScore());
        assertNotNull(done.verdictLabel());
        assertTrue(done.contractText().contains("Tiền điện 4.000đ/kWh"));
        assertEquals(1, done.findings().size());
        assertEquals("RED", done.findings().get(0).riskLevel());
        assertNotNull(done.findings().get(0).lawRef());   // căn cứ luật do code gắn
        assertEquals(14, done.checklist().size());
        assertTrue(done.checklist().stream()
                .anyMatch(c -> c.clauseType().equals("HOAN_COC") && !c.present()));
    }

    @Test
    void hallucinatedFindingIsDroppedByGrounding() {
        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"))));
        when(analyzer.analyze(anyList())).thenReturn(List.of(
                new RawFinding(ClauseType.DON_PHUONG_CHAM_DUT, RiskLevel.RED, "Chủ nhà đuổi bất cứ lúc nào",
                        "Điều khoản bịa", null)));

        AnalysisResponse created = analysisService.create(List.of(jpg("hopdong.jpg")));
        AnalysisResponse done = analysisService.get(created.id());

        assertEquals("COMPLETED", done.status());
        assertTrue(done.findings().isEmpty());   // finding bịa bị loại
    }

    @Test
    void unreadableImageBecomesFailedWithReason() {
        when(extractor.extract(anyList())).thenReturn(
                new ExtractionResult(false, "Ảnh bị mờ, không đọc được chữ", List.of()));

        AnalysisResponse created = analysisService.create(List.of(jpg("mo.jpg")));
        AnalysisResponse done = analysisService.get(created.id());

        assertEquals("FAILED", done.status());
        assertEquals("Ảnh bị mờ, không đọc được chữ", done.errorMessage());
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
        // Lỗi AI xảy ra trên luồng nền -> create() KHÔNG ném ra ngoài nữa,
        // nhưng bản ghi phải chuyển FAILED để trang lịch sử hiển thị tử tế.
        when(extractor.extract(anyList()))
                .thenThrow(new BadRequestException("Đã hết lượt AI miễn phí hôm nay — hãy thử lại sau."));

        AnalysisResponse created = analysisService.create(List.of(jpg("a.jpg")));
        AnalysisResponse done = analysisService.get(created.id());

        assertEquals("FAILED", done.status());
        assertTrue(done.errorMessage().contains("hết lượt AI"));
    }

    @Test
    void getRejectsAnotherUsersAnalysis() {
        // User A tạo lượt soát (gắn tài khoản A)
        userRepository.save(new com.example.soattro.entity.User("owner@test.local", "hash"));
        loginAs("owner@test.local");
        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"))));
        when(analyzer.analyze(anyList())).thenReturn(List.of());
        AnalysisResponse created = analysisService.create(List.of(jpg("a.jpg")));

        // Người khác (ẩn danh) đoán id -> KHÔNG được đọc hợp đồng của A
        SecurityContextHolder.clearContext();
        Long id = created.id();
        assertThrows(com.example.soattro.exception.ResourceNotFoundException.class,
                () -> analysisService.get(id));
    }

    @Test
    void historyRequiresLogin() {
        // Ẩn danh -> 401
        assertThrows(UnauthorizedException.class, () -> analysisService.history());
    }

    @Test
    void historyReturnsLoggedInUserAnalyses() {
        userRepository.save(new com.example.soattro.entity.User("history@test.local", "hash"));
        loginAs("history@test.local");

        when(extractor.extract(anyList())).thenReturn(new ExtractionResult(true, "", List.of(
                new ExtractedClause(ClauseType.GIA_DIEN, "Tiền điện 4.000đ/kWh"))));
        when(analyzer.analyze(anyList())).thenReturn(List.of());

        AnalysisResponse created = analysisService.create(List.of(jpg("a.jpg")));

        List<AnalysisSummary> history = analysisService.history();
        assertEquals(1, history.size());
        assertEquals(created.id(), history.get(0).id());
        assertEquals("COMPLETED", history.get(0).status());
        assertNotNull(history.get(0).verdictLabel());
    }
}

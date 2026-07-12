package com.example.soattro;

import com.example.soattro.ai.RawFinding;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import com.example.soattro.service.ScoreCalculator;
import com.example.soattro.service.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Công thức chấm điểm tất định (rubric mục 5) — test từng nhánh để đảm bảo tái lập.
 */
class ScoreCalculatorTest {

    private final ScoreCalculator calc = new ScoreCalculator();

    private RawFinding f(ClauseType type, RiskLevel risk) {
        return new RawFinding(type, risk, "quote", "giai thich", null);
    }

    @Test
    void perfectContractScores100() {
        ScoreResult r = calc.calculate(List.of(), List.of());
        assertEquals(100, r.score());
        assertTrue(r.verdictLabel().contains("Khá an toàn"));
    }

    @Test
    void oneRedSubtractsWeight() {
        // GIA_DIEN W=10 -> 100-10 = 90
        ScoreResult r = calc.calculate(List.of(f(ClauseType.GIA_DIEN, RiskLevel.RED)), List.of());
        assertEquals(90, r.score());
    }

    @Test
    void oneYellowSubtracts40PercentWeightRounded() {
        // HOAN_COC W=12 -> round(0.4*12)=5 -> 100-5 = 95
        ScoreResult r = calc.calculate(List.of(f(ClauseType.HOAN_COC, RiskLevel.YELLOW)), List.of());
        assertEquals(95, r.score());
    }

    @Test
    void greenDoesNotSubtract() {
        ScoreResult r = calc.calculate(List.of(f(ClauseType.NOI_QUY_SU_DUNG, RiskLevel.GREEN)), List.of());
        assertEquals(100, r.score());
    }

    @Test
    void sameTypeCountedOnceTakingWorst() {
        // Hai finding cùng GIA_DIEN (YELLOW + RED) -> chỉ trừ 1 lần theo RED (W=10) = 90
        ScoreResult r = calc.calculate(List.of(
                f(ClauseType.GIA_DIEN, RiskLevel.YELLOW),
                f(ClauseType.GIA_DIEN, RiskLevel.RED)), List.of());
        assertEquals(90, r.score());
    }

    @Test
    void missingEssentialSubtractsPenalty() {
        // Thiếu HOAN_COC M=10 -> 90
        ScoreResult r = calc.calculate(List.of(), List.of(ClauseType.HOAN_COC));
        assertEquals(90, r.score());
    }

    @Test
    void scoreClampedAtZero() {
        // Nhiều RED nặng -> không âm
        ScoreResult r = calc.calculate(List.of(
                f(ClauseType.HOAN_COC, RiskLevel.RED),          // 12
                f(ClauseType.DON_PHUONG_CHAM_DUT, RiskLevel.RED), // 12
                f(ClauseType.TANG_GIA, RiskLevel.RED),          // 10
                f(ClauseType.DAT_COC, RiskLevel.RED),           // 10
                f(ClauseType.GIA_DIEN, RiskLevel.RED),          // 10
                f(ClauseType.GIA_THUE_THANH_TOAN, RiskLevel.RED), // 8
                f(ClauseType.PHI_DICH_VU_KHAC, RiskLevel.RED),  // 8
                f(ClauseType.THOI_HAN_THUE, RiskLevel.RED),     // 8
                f(ClauseType.PHAT_VI_PHAM, RiskLevel.RED),      // 8
                f(ClauseType.NOI_QUY_SU_DUNG, RiskLevel.RED)),  // 6  => tổng 92
                List.of(ClauseType.HIEN_TRANG_TAI_SAN));        // +8 = 100 trừ
        assertEquals(0, r.score());
        assertTrue(r.verdictLabel().contains("Rủi ro cao"));
    }

    @Test
    void verdictThresholds() {
        assertTrue(calc.labelFor(85).contains("Khá an toàn"));
        assertTrue(calc.labelFor(60).contains("Cần làm rõ"));
        assertTrue(calc.labelFor(30).contains("Rủi ro cao"));
        // Biên: 80 -> xanh, 79 -> vàng, 50 -> vàng, 49 -> đỏ
        assertTrue(calc.labelFor(80).contains("Khá an toàn"));
        assertTrue(calc.labelFor(79).contains("Cần làm rõ"));
        assertTrue(calc.labelFor(50).contains("Cần làm rõ"));
        assertTrue(calc.labelFor(49).contains("Rủi ro cao"));
    }
}

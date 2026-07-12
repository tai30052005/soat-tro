package com.example.soattro;

import com.example.soattro.ai.RawFinding;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import com.example.soattro.service.GroundingVerifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lá chắn chống AI bịa — test kỹ vì đây là quyết định thiết kế số 2 của pipeline.
 */
class GroundingVerifierTest {

    private final GroundingVerifier verifier = new GroundingVerifier();
    private static final String CONTRACT =
            "Dieu 3. Tien dien: 4.500 dong/kWh.\n\nDieu 5. Coc 2 thang tien nha.";

    private RawFinding finding(String quote) {
        return new RawFinding(ClauseType.GIA_DIEN, RiskLevel.RED, quote, "giai thich", null);
    }

    @Test
    void keepsFindingWithVerbatimQuote() {
        List<RawFinding> kept = verifier.verify(CONTRACT, List.of(finding("4.500 dong/kWh")));
        assertEquals(1, kept.size());
    }

    @Test
    void dropsHallucinatedQuote() {
        // Câu này KHÔNG có trong hợp đồng -> phải bị loại
        List<RawFinding> kept = verifier.verify(CONTRACT, List.of(finding("Tien dien 10.000 dong/kWh")));
        assertTrue(kept.isEmpty());
    }

    @Test
    void matchesDespiteCaseAndExtraWhitespace() {
        // OCR chèn khoảng trắng thừa / khác hoa-thường vẫn phải khớp
        List<RawFinding> kept = verifier.verify(CONTRACT, List.of(finding("TIEN DIEN:   4.500   dong/kWh")));
        assertEquals(1, kept.size());
    }

    @Test
    void dropsEmptyQuote() {
        assertTrue(verifier.verify(CONTRACT, List.of(finding("   "))).isEmpty());
    }

    @Test
    void keepsOnlyGroundedAmongMixed() {
        List<RawFinding> kept = verifier.verify(CONTRACT, List.of(
                finding("Coc 2 thang tien nha"),      // có thật
                finding("Phi quan ly 500k")));         // bịa
        assertEquals(1, kept.size());
        assertEquals("Coc 2 thang tien nha", kept.get(0).quote());
    }
}

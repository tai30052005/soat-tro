package com.example.soattro;

import com.example.soattro.ai.ClauseAnalyzer;
import com.example.soattro.ai.GeminiClient;
import com.example.soattro.ai.RawFinding;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Test phần PARSE JSON findings của Gemini (không gọi API thật). */
class ClauseAnalyzerTest {

    private final ClauseAnalyzer analyzer = new ClauseAnalyzer(mock(GeminiClient.class));

    @Test
    void parseHappyPath() {
        String json = """
                {"findings": [
                  {"clauseType": "GIA_DIEN", "riskLevel": "RED", "quote": "4.500d/kWh",
                   "explanation": "Gia dien cao hon quy dinh", "suggestion": "Hoi lai chu tro"},
                  {"clauseType": "DAT_COC", "riskLevel": "YELLOW", "quote": "coc 2 thang",
                   "explanation": "Coc cao hon thong le"}
                ]}
                """;
        List<RawFinding> findings = analyzer.parse(json);

        assertEquals(2, findings.size());
        assertEquals(ClauseType.GIA_DIEN, findings.get(0).clauseType());
        assertEquals(RiskLevel.RED, findings.get(0).riskLevel());
        assertEquals("Hoi lai chu tro", findings.get(0).suggestion());
    }

    @Test
    void unknownEnumsFallBackSafely() {
        String json = """
                {"findings": [
                  {"clauseType": "MA_LA", "riskLevel": "PURPLE", "quote": "x",
                   "explanation": "gi do"}
                ]}
                """;
        List<RawFinding> findings = analyzer.parse(json);

        assertEquals(ClauseType.OTHER, findings.get(0).clauseType());
        assertEquals(RiskLevel.YELLOW, findings.get(0).riskLevel());   // không rõ -> vàng, không bỏ sót
    }

    @Test
    void skipsFindingWithoutExplanation() {
        String json = """
                {"findings": [
                  {"clauseType": "GIA_DIEN", "riskLevel": "RED", "quote": "x", "explanation": "  "}
                ]}
                """;
        assertTrue(analyzer.parse(json).isEmpty());
    }

    @Test
    void malformedJsonReturnsEmpty() {
        assertTrue(analyzer.parse("khong phai json").isEmpty());
    }
}

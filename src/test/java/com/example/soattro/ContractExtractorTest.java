package com.example.soattro;

import com.example.soattro.ai.ContractExtractor;
import com.example.soattro.ai.ExtractionResult;
import com.example.soattro.ai.GeminiClient;
import com.example.soattro.entity.ClauseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit test cho phần PARSE kết quả Gemini (không gọi API thật).
 * Đây là ranh giới dễ vỡ nhất: JSON của model -> object của mình.
 */
class ContractExtractorTest {

    // GeminiClient được mock vì chỉ test parse; extract() có test tích hợp thủ công.
    private final ContractExtractor extractor = new ContractExtractor(mock(GeminiClient.class));

    @Test
    void parseHappyPath() {
        String json = """
                {"readable": true, "reason": "", "clauses": [
                  {"clauseType": "GIA_DIEN", "text": "Tiền điện 4.000đ/kWh"},
                  {"clauseType": "DAT_COC", "text": "Đặt cọc 5.000.000 đồng"}
                ]}
                """;
        ExtractionResult result = extractor.parse(json);

        assertTrue(result.readable());
        assertEquals(2, result.clauses().size());
        assertEquals(ClauseType.GIA_DIEN, result.clauses().get(0).clauseType());
        assertEquals("Tiền điện 4.000đ/kWh", result.clauses().get(0).text());
    }

    @Test
    void parseUnreadableImage() {
        String json = """
                {"readable": false, "reason": "Ảnh bị mờ, không đọc được chữ", "clauses": []}
                """;
        ExtractionResult result = extractor.parse(json);

        assertFalse(result.readable());
        assertEquals("Ảnh bị mờ, không đọc được chữ", result.reason());
        assertTrue(result.clauses().isEmpty());
    }

    @Test
    void unknownClauseTypeFallsBackToOther() {
        // Model "sáng tạo" mã ngoài taxonomy -> quy về OTHER, không ném exception
        String json = """
                {"readable": true, "clauses": [
                  {"clauseType": "MA_LA_HOAC", "text": "Điều khoản gì đó"}
                ]}
                """;
        ExtractionResult result = extractor.parse(json);

        assertEquals(ClauseType.OTHER, result.clauses().get(0).clauseType());
    }

    @Test
    void readableButNoClausesBecomesUnreadable() {
        // readable=true nhưng mảng rỗng là vô nghĩa -> chuyển thành thất bại có lý do
        ExtractionResult result = extractor.parse("""
                {"readable": true, "clauses": []}
                """);

        assertFalse(result.readable());
        assertFalse(result.reason().isBlank());
    }

    @Test
    void malformedJsonHandledGracefully() {
        ExtractionResult result = extractor.parse("đây không phải json");

        assertFalse(result.readable());
        assertTrue(result.clauses().isEmpty());
    }

    @Test
    void blankClauseTextIsSkipped() {
        String json = """
                {"readable": true, "clauses": [
                  {"clauseType": "GIA_NUOC", "text": "   "},
                  {"clauseType": "GIA_NUOC", "text": "Nước 100k/người"}
                ]}
                """;
        ExtractionResult result = extractor.parse(json);

        assertEquals(1, result.clauses().size());
    }
}

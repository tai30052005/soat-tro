package com.example.soattro.ai;

import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BƯỚC 2 của pipeline: gửi các điều khoản đã bóc (bước 1) cho Gemini, yêu cầu
 * đánh giá rủi ro TỪNG điều khoản theo rubric → danh sách RawFinding.
 *
 * Ràng buộc chống bịa: mỗi finding BẮT BUỘC kèm "quote" copy nguyên văn từ điều
 * khoản. Code (GroundingVerifier) sẽ loại finding có quote không khớp hợp đồng gốc.
 * lawRef KHÔNG do model sinh — code tự gắn từ rubric (chống trích luật sai).
 */
@Component
public class ClauseAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ClauseAnalyzer.class);

    private final GeminiClient gemini;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClauseAnalyzer(GeminiClient gemini) {
        this.gemini = gemini;
    }

    public List<RawFinding> analyze(List<ExtractedClause> clauses) {
        if (clauses.isEmpty()) {
            return List.of();
        }
        String prompt = buildPrompt(clauses);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema(),
                        "temperature", 0));

        JsonNode response = gemini.generate(body);
        return parse(gemini.firstText(response));
    }

    /** Tách riêng public để unit test không cần gọi API thật. */
    public List<RawFinding> parse(String json) {
        List<RawFinding> findings = new ArrayList<>();
        if (json == null || json.isBlank()) {
            log.warn("ClauseAnalyzer: Gemini trả về rỗng");
            return findings;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode f : root.path("findings")) {
                ClauseType type = parseType(f.path("clauseType").asText(""));
                RiskLevel risk = parseRisk(f.path("riskLevel").asText(""));
                String quote = f.path("quote").asText("").trim();
                String explanation = f.path("explanation").asText("").trim();
                String suggestion = f.path("suggestion").asText("").trim();
                if (explanation.isEmpty()) {
                    continue;   // finding không có giải thích thì vô nghĩa, bỏ
                }
                findings.add(new RawFinding(type, risk, quote, explanation,
                        suggestion.isEmpty() ? null : suggestion));
            }
        } catch (Exception e) {
            log.warn("ClauseAnalyzer: không parse được JSON: {}", e.getMessage());
        }
        return findings;
    }

    private ClauseType parseType(String raw) {
        try {
            return ClauseType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ClauseType.OTHER;
        }
    }

    private RiskLevel parseRisk(String raw) {
        try {
            return RiskLevel.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return RiskLevel.YELLOW;   // không rõ thì coi là cần làm rõ, không bỏ sót
        }
    }

    private Map<String, Object> responseSchema() {
        List<String> typeNames = Arrays.stream(ClauseType.values()).map(Enum::name).toList();
        List<String> riskNames = Arrays.stream(RiskLevel.values()).map(Enum::name).toList();

        Map<String, Object> findingSchema = new LinkedHashMap<>();
        findingSchema.put("type", "OBJECT");
        findingSchema.put("properties", Map.of(
                "clauseType", Map.of("type", "STRING", "enum", typeNames),
                "riskLevel", Map.of("type", "STRING", "enum", riskNames),
                "quote", Map.of("type", "STRING"),
                "explanation", Map.of("type", "STRING"),
                "suggestion", Map.of("type", "STRING")));
        findingSchema.put("required", List.of("clauseType", "riskLevel", "quote", "explanation"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", Map.of(
                "findings", Map.of("type", "ARRAY", "items", findingSchema)));
        schema.put("required", List.of("findings"));
        return schema;
    }

    private String buildPrompt(List<ExtractedClause> clauses) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Bạn là chuyên gia soát hợp đồng thuê trọ, bảo vệ quyền lợi NGƯỜI THUÊ.
                Dưới đây là các điều khoản đã bóc từ một hợp đồng. Hãy đánh giá rủi ro TỪNG điều khoản.

                Với mỗi điều khoản, tạo một finding gồm:
                - clauseType: giữ đúng mã loại đã cho.
                - riskLevel: RED (bất lợi rõ ràng / trái luật), YELLOW (mơ hồ, cần làm rõ), GREEN (ổn).
                - quote: COPY NGUYÊN VĂN đoạn chữ trong điều khoản làm bằng chứng (bắt buộc, không tự viết lại).
                - explanation: giải thích DỄ HIỂU cho sinh viên thuê trọ vì sao đánh giá vậy (1-3 câu, không dùng thuật ngữ luật khô khan).
                - suggestion: câu người thuê nên hỏi/thương lượng với chủ trọ (bỏ trống nếu GREEN).
                KHÔNG trích số điều luật trong explanation — hệ thống tự gắn căn cứ pháp lý sau.

                TIÊU CHÍ RỦI RO THEO TỪNG LOẠI (áp đúng, đừng bịa thêm):
                - THONG_TIN_CAC_BEN: RED nếu bắt nộp/giữ bản gốc CCCD của người thuê, hoặc người ký không phải chủ và không có ủy quyền. YELLOW nếu thiếu thông tin liên hệ.
                - DOI_TUONG_THUE: RED nếu không ghi địa chỉ phòng cụ thể. YELLOW nếu thiếu diện tích/tình trạng.
                - GIA_THUE_THANH_TOAN: RED nếu không ghi số tiền cụ thể. YELLOW nếu thiếu kỳ hạn/phương thức thanh toán.
                - TANG_GIA: RED nếu cho chủ nhà tăng giá tùy ý / không báo trước / trong thời hạn hợp đồng. YELLOW nếu có nói tăng nhưng không rõ mức trần & thời điểm.
                - DAT_COC: RED nếu 'cọc thuộc về chủ nhà trong mọi trường hợp'. YELLOW nếu cọc từ 2 tháng trở lên hoặc không ghi rõ mục đích cọc.
                - HOAN_COC: RED nếu điều kiện hoàn cọc mơ hồ ('tùy chủ nhà'), hoặc trả phòng đúng hẹn vẫn mất cọc. YELLOW nếu không ghi số ngày hoàn cọc, hoặc khấu trừ không gắn biên bản.
                - GIA_DIEN: RED nếu giá điện > ~3.800đ/kWh hoặc 'do chủ nhà quy định'. YELLOW nếu 3.000-3.800đ/kWh không nêu căn cứ, hoặc gộp điện khu chung vào đơn giá.
                - GIA_NUOC: RED nếu > 18.000đ/m3 hoặc 'do chủ nhà quy định'. YELLOW nếu khoán đầu người không nêu định mức.
                - PHI_DICH_VU_KHAC: RED nếu có khoản 'phí phát sinh khác theo thông báo của chủ nhà' (mở, vô hạn). YELLOW nếu liệt kê phí nhưng không ghi số tiền.
                - THOI_HAN_THUE: RED nếu không ghi thời hạn VÀ chủ được lấy phòng bất kỳ lúc nào. YELLOW nếu thiếu thời hạn hoặc điều kiện gia hạn.
                - DON_PHUONG_CHAM_DUT: RED nếu chủ được chấm dứt không lý do/không báo trước, hoặc quyền chấm dứt một phía bất cân xứng. YELLOW nếu báo trước dưới 30 ngày hoặc không nêu trường hợp cụ thể.
                - HIEN_TRANG_TAI_SAN: RED nếu quy mọi hư hỏng (kể cả hao mòn tự nhiên) cho người thuê. YELLOW nếu không có biên bản bàn giao ghi tình trạng.
                - SUA_CHUA_BAO_TRI: RED nếu đẩy toàn bộ sửa chữa (kể cả hư hỏng lớn/kết cấu) cho người thuê. YELLOW nếu không phân định hư hỏng lớn/nhỏ.
                - TAM_TRU_PHAP_LY: RED nếu chủ từ chối hỗ trợ đăng ký tạm trú. YELLOW nếu không đề cập tạm trú.
                - NOI_QUY_SU_DUNG: RED nếu chủ được tự ý vào phòng bất kỳ lúc nào, hoặc được khóa phòng/giữ đồ để xiết nợ. YELLOW nếu nội quy cấm đoán mơ hồ, phạt tùy ý.
                - PHAT_VI_PHAM: RED nếu phạt một chiều chỉ người thuê hoặc mức phạt phi lý. YELLOW nếu có phạt nhưng không ghi mức.
                - OTHER: luôn GREEN, explanation ngắn gọn.

                Trả về JSON đúng schema. CÁC ĐIỀU KHOẢN:
                """);
        for (int i = 0; i < clauses.size(); i++) {
            ExtractedClause c = clauses.get(i);
            sb.append(i + 1).append(". [").append(c.clauseType().name()).append("] ")
                    .append(c.text()).append("\n");
        }
        return sb.toString();
    }
}

package com.example.soattro.ai;

import com.example.soattro.entity.ClauseType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BƯỚC 1 của pipeline AI: gửi ảnh/PDF hợp đồng cho Gemini Vision, nhận về
 * danh sách điều khoản NGUYÊN VĂN đã phân loại theo taxonomy cố định.
 *
 * 2 kỹ thuật then chốt (điểm kể khi phỏng vấn):
 *  1. Prompt MULTIMODAL: parts = [ảnh base64..., text hướng dẫn] trong 1 request.
 *  2. STRUCTURED OUTPUT: khai báo responseSchema (kiểu OpenAPI) + responseMimeType
 *     application/json -> Gemini BẮT BUỘC trả JSON đúng khung, clauseType bị ép
 *     vào enum 17 mã -> không còn cảnh parse văn xuôi tự do của LLM.
 */
@Component
public class ContractExtractor {

    private static final Logger log = LoggerFactory.getLogger(ContractExtractor.class);

    private final GeminiClient gemini;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContractExtractor(GeminiClient gemini) {
        this.gemini = gemini;
    }

    /** File đầu vào tối giản (không phụ thuộc MultipartFile để dễ unit test). */
    public record InputFile(String mimeType, byte[] data) {
    }

    /** Gọi Gemini bóc tách hợp đồng từ danh sách ảnh/PDF. */
    public ExtractionResult extract(List<InputFile> files) {
        List<Map<String, Object>> parts = new ArrayList<>();
        // Ảnh đưa lên TRƯỚC, hướng dẫn text đặt SAU (thứ tự Google khuyến nghị
        // cho tác vụ "đọc tài liệu trong ảnh").
        for (InputFile f : files) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", f.mimeType(),
                    "data", Base64.getEncoder().encodeToString(f.data()))));
        }
        parts.add(Map.of("text", buildPrompt()));

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of(
                        // Ép Gemini trả JSON đúng schema thay vì văn xuôi
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema(),
                        // temperature 0: tác vụ bóc tách cần TÁI LẬP, không cần sáng tạo
                        "temperature", 0));

        JsonNode response = gemini.generate(body);
        String json = gemini.firstText(response);
        return parse(json);
    }

    /**
     * Đọc JSON Gemini trả về thành ExtractionResult.
     * Tách riêng public để unit test không cần gọi API thật.
     */
    public ExtractionResult parse(String json) {
        if (json == null || json.isBlank()) {
            log.warn("Gemini trả về rỗng");
            return new ExtractionResult(false, "AI không trả về kết quả, hãy thử lại.", List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean readable = root.path("readable").asBoolean(false);
            String reason = root.path("reason").asText("");

            List<ExtractedClause> clauses = new ArrayList<>();
            for (JsonNode c : root.path("clauses")) {
                String text = c.path("text").asText("").trim();
                if (text.isEmpty()) {
                    continue;   // bỏ mục rỗng
                }
                clauses.add(new ExtractedClause(parseType(c.path("clauseType").asText("")), text));
            }

            if (readable && clauses.isEmpty()) {
                // Schema đúng nhưng không bóc được gì -> coi như không đọc được
                return new ExtractionResult(false,
                        "Không nhận diện được điều khoản nào trong tài liệu.", List.of());
            }
            return new ExtractionResult(readable, reason, clauses);
        } catch (Exception e) {
            log.warn("Không parse được JSON từ Gemini: {}", e.getMessage());
            return new ExtractionResult(false, "AI trả về dữ liệu không hợp lệ, hãy thử lại.", List.of());
        }
    }

    /** Mã lạ ngoài enum (phòng khi model "sáng tạo") -> quy về OTHER, không vỡ pipeline. */
    private ClauseType parseType(String raw) {
        try {
            return ClauseType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ClauseType.OTHER;
        }
    }

    /** Schema kiểu OpenAPI cho structured output — clauseType bị ép vào enum taxonomy. */
    private Map<String, Object> responseSchema() {
        List<String> typeNames = Arrays.stream(ClauseType.values()).map(Enum::name).toList();

        Map<String, Object> clauseSchema = new LinkedHashMap<>();
        clauseSchema.put("type", "OBJECT");
        clauseSchema.put("properties", Map.of(
                "clauseType", Map.of("type", "STRING", "enum", typeNames),
                "text", Map.of("type", "STRING")));
        clauseSchema.put("required", List.of("clauseType", "text"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", Map.of(
                "readable", Map.of("type", "BOOLEAN"),
                "reason", Map.of("type", "STRING"),
                "clauses", Map.of("type", "ARRAY", "items", clauseSchema)));
        schema.put("required", List.of("readable", "clauses"));
        return schema;
    }

    private String buildPrompt() {
        return """
                Bạn là công cụ bóc tách hợp đồng thuê trọ/thuê nhà tiếng Việt từ ảnh chụp hoặc PDF.

                NHIỆM VỤ: đọc toàn bộ tài liệu (có thể nhiều trang, theo đúng thứ tự ảnh) và tách thành
                danh sách điều khoản.

                QUY TẮC BẮT BUỘC:
                1. "text" phải là NGUYÊN VĂN trong tài liệu — giữ đúng chính tả, viết hoa, số liệu,
                   kể cả lỗi chính tả của người viết. TUYỆT ĐỐI không sửa, không tóm tắt, không suy diễn.
                2. Mỗi điều khoản/đoạn có nội dung độc lập là một mục. Điều khoản dài chứa nhiều
                   chủ đề (vd vừa giá điện vừa giá nước) thì tách thành nhiều mục theo từng chủ đề.
                3. "clauseType" chọn đúng 1 mã trong danh sách; không chắc chắn thì dùng OTHER:
                   - THONG_TIN_CAC_BEN: họ tên, CCCD, địa chỉ của bên thuê/bên cho thuê
                   - DOI_TUONG_THUE: mô tả phòng (địa chỉ, số phòng, diện tích, trang bị)
                   - GIA_THUE_THANH_TOAN: số tiền thuê, kỳ hạn và cách thanh toán
                   - TANG_GIA: điều chỉnh/tăng giá thuê
                   - DAT_COC: số tiền cọc, mục đích cọc
                   - HOAN_COC: điều kiện, thời hạn hoàn/khấu trừ cọc
                   - GIA_DIEN: giá điện, cách tính điện
                   - GIA_NUOC: giá nước, cách tính nước
                   - PHI_DICH_VU_KHAC: rác, giữ xe, quản lý, internet, thang máy...
                   - THOI_HAN_THUE: thời hạn thuê, gia hạn
                   - DON_PHUONG_CHAM_DUT: chấm dứt hợp đồng trước hạn, thời gian báo trước
                   - HIEN_TRANG_TAI_SAN: biên bản bàn giao, danh mục nội thất, hiện trạng phòng
                   - SUA_CHUA_BAO_TRI: trách nhiệm sửa chữa hư hỏng
                   - TAM_TRU_PHAP_LY: đăng ký tạm trú, khai báo cư trú
                   - NOI_QUY_SU_DUNG: giờ giấc, khách, vật nuôi, quyền vào phòng của chủ nhà
                   - PHAT_VI_PHAM: phạt vi phạm, bồi thường
                   - OTHER: không thuộc loại nào ở trên
                4. Nếu ảnh quá mờ, thiếu trang rõ rệt, hoặc tài liệu KHÔNG phải hợp đồng thuê
                   trọ/thuê nhà: trả readable=false và ghi lý do ngắn gọn, dễ hiểu vào "reason"
                   (vd "Ảnh bị mờ, không đọc được chữ" / "Tài liệu không phải hợp đồng thuê trọ").
                   Khi đó để clauses là mảng rỗng.
                5. Đọc được bình thường: readable=true, reason để trống.
                """;
    }
}

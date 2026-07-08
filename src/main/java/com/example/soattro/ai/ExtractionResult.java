package com.example.soattro.ai;

import java.util.List;

/**
 * Kết quả bước 1 của pipeline (Gemini Vision đọc hợp đồng).
 *
 * readable = false khi ảnh mờ / không phải hợp đồng thuê trọ — kèm reason
 * để trả lời người dùng đàng hoàng thay vì phân tích bừa (rủi ro số 2 trong SPEC).
 */
public record ExtractionResult(boolean readable, String reason, List<ExtractedClause> clauses) {
}

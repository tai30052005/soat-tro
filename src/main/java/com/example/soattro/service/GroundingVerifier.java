package com.example.soattro.service;

import com.example.soattro.ai.RawFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * VERIFY GROUNDING (bước 3, code thuần) — quyết định thiết kế số 2 của pipeline.
 *
 * Loại mọi finding có "quote" KHÔNG xuất hiện nguyên văn trong contract_text.
 * Đây là lá chắn chống AI bịa: nếu model tự nghĩ ra một câu không có trong hợp đồng,
 * finding đó bị vứt, không bao giờ hiển thị cho người dùng hay ảnh hưởng điểm.
 *
 * So khớp có CHUẨN HÓA nhẹ (thường/hoa, gộp khoảng trắng) để không loại nhầm khi
 * OCR chèn khoảng trắng thừa; nhưng vẫn yêu cầu trùng chuỗi con — không so mờ.
 */
@Component
public class GroundingVerifier {

    private static final Logger log = LoggerFactory.getLogger(GroundingVerifier.class);

    /** Giữ lại các finding có quote khớp hợp đồng gốc; loại phần còn lại. */
    public List<RawFinding> verify(String contractText, List<RawFinding> findings) {
        String haystack = normalize(contractText);
        List<RawFinding> kept = new ArrayList<>();
        for (RawFinding f : findings) {
            String quote = f.quote() == null ? "" : normalize(f.quote());
            if (quote.isBlank()) {
                log.debug("Loại finding {} — quote rỗng", f.clauseType());
                continue;
            }
            if (haystack.contains(quote)) {
                kept.add(f);
            } else {
                log.debug("Loại finding {} — quote không khớp hợp đồng: '{}'", f.clauseType(), f.quote());
            }
        }
        return kept;
    }

    /** Thường hóa + gộp mọi chuỗi khoảng trắng thành 1 dấu cách + trim. */
    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}

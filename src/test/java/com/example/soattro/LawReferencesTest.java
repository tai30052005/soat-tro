package com.example.soattro;

import com.example.soattro.entity.ClauseType;
import com.example.soattro.service.LawReferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Căn cứ pháp lý do CODE gắn (chống rủi ro "AI trích luật sai"). Test bảo đảm bảng tra
 * không bao giờ trả chuỗi rỗng, và các loại rủi ro chính đều có điều luật.
 */
class LawReferencesTest {

    private final LawReferences laws = new LawReferences();

    @Test
    void neverReturnsBlankReference() {
        // Cho phép null (loại không gắn luật), nhưng không được là chuỗi rỗng/toàn khoảng trắng.
        for (ClauseType t : ClauseType.values()) {
            String ref = laws.forType(t);
            if (ref != null) {
                assertTrue(!ref.isBlank(), t + " có căn cứ luật rỗng");
            }
        }
    }

    @Test
    void coreRiskTypesHaveLawReference() {
        // Những loại rủi ro cao nhất phải có điều luật để hiển thị cho người dùng.
        for (ClauseType t : new ClauseType[]{
                ClauseType.DAT_COC, ClauseType.HOAN_COC,
                ClauseType.TANG_GIA, ClauseType.DON_PHUONG_CHAM_DUT}) {
            assertNotNull(laws.forType(t), t + " thiếu căn cứ luật");
        }
    }

    @Test
    void depositTypesCiteArticle328() {
        // Đặt cọc & hoàn cọc cùng căn cứ Điều 328 BLDS 2015 — chốt để không bị đổi nhầm.
        assertTrue(laws.forType(ClauseType.DAT_COC).contains("328"));
        assertTrue(laws.forType(ClauseType.HOAN_COC).contains("328"));
    }

    @Test
    void unclassifiedTypeHasNoLaw() {
        // OTHER không phải vi phạm luật cụ thể -> không gắn điều luật.
        assertNull(laws.forType(ClauseType.OTHER));
    }
}

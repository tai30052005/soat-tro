package com.example.soattro;

import com.example.soattro.entity.ClauseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bất biến của TAXONOMY (quyết định thiết kế số 1). Rubric nằm trong enum, nên
 * một chỉnh sửa lơ đễnh (đặt weight=0, quên missingPenalty...) sẽ làm điểm sai
 * mà không báo lỗi. Các test này là hàng rào chống hồi quy cho công thức chấm điểm.
 */
class ClauseTypeInvariantsTest {

    @Test
    void essentialTypesPenalizeBothRiskAndAbsence() {
        // Loại thiết yếu phải trừ điểm khi có RED (weight>0) VÀ khi bị thiếu (missingPenalty>0),
        // nếu không, ScoreCalculator/ChecklistBuilder mất tác dụng với loại đó.
        for (ClauseType t : ClauseType.values()) {
            if (t.isEssential()) {
                assertTrue(t.getWeight() > 0, t + " thiết yếu nhưng weight = 0");
                assertTrue(t.getMissingPenalty() > 0, t + " thiết yếu nhưng missingPenalty = 0");
            }
        }
    }

    @Test
    void nonEssentialTypesHaveNoMissingPenalty() {
        // Loại không thiết yếu không vào checklist "thiếu" -> không được trừ điểm khi vắng.
        for (ClauseType t : ClauseType.values()) {
            if (!t.isEssential()) {
                assertEquals(0, t.getMissingPenalty(),
                        t + " không thiết yếu nhưng vẫn có missingPenalty");
            }
        }
    }

    @Test
    void allWeightsNonNegativeAndLabelsPresent() {
        for (ClauseType t : ClauseType.values()) {
            assertTrue(t.getWeight() >= 0, t + " có weight âm");
            assertTrue(t.getMissingPenalty() >= 0, t + " có missingPenalty âm");
            assertFalse(t.getLabel() == null || t.getLabel().isBlank(),
                    t + " thiếu label hiển thị");
        }
    }

    @Test
    void otherIsNeutral() {
        // OTHER = điều khoản không phân loại được: không chấm rủi ro, không tính thiếu.
        assertFalse(ClauseType.OTHER.isEssential());
        assertEquals(0, ClauseType.OTHER.getWeight());
        assertEquals(0, ClauseType.OTHER.getMissingPenalty());
    }
}

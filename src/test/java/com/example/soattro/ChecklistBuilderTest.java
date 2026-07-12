package com.example.soattro;

import com.example.soattro.entity.ClauseType;
import com.example.soattro.service.ChecklistBuilder;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistBuilderTest {

    private final ChecklistBuilder builder = new ChecklistBuilder();

    @Test
    void marksPresentAndMissingEssentialTypes() {
        Map<ClauseType, Boolean> result = builder.build(
                EnumSet.of(ClauseType.GIA_DIEN, ClauseType.DAT_COC));

        assertTrue(result.get(ClauseType.GIA_DIEN));   // có mặt
        assertTrue(result.get(ClauseType.DAT_COC));
        assertFalse(result.get(ClauseType.HOAN_COC));  // thiết yếu nhưng thiếu
    }

    @Test
    void onlyEssentialTypesIncluded() {
        Map<ClauseType, Boolean> result = builder.build(EnumSet.noneOf(ClauseType.class));

        // 14 loại thiết yếu trong taxonomy
        assertEquals(14, result.size());
        // Loại KHÔNG thiết yếu không nằm trong checklist
        assertNull(result.get(ClauseType.NOI_QUY_SU_DUNG));
        assertNull(result.get(ClauseType.PHAT_VI_PHAM));
        assertNull(result.get(ClauseType.OTHER));
    }
}

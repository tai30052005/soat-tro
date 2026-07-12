package com.example.soattro;

import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.service.ChecklistBuilder;
import com.example.soattro.service.GroundingVerifier;
import com.example.soattro.service.LawReferences;
import com.example.soattro.service.SampleContractProvider;
import com.example.soattro.service.ScoreCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hợp đồng mẫu phải chạy qua ĐÚNG pipeline tất định và cho kết quả trung thực:
 * mọi finding soạn sẵn đều grounded, có điều khoản thiếu, điểm phản ánh rủi ro cao.
 */
class SampleContractProviderTest {

    private final SampleContractProvider provider = new SampleContractProvider(
            new GroundingVerifier(), new ChecklistBuilder(), new ScoreCalculator(), new LawReferences());

    @Test
    void sampleIsFullyGroundedAndScored() {
        AnalysisResponse r = provider.build();

        assertEquals("COMPLETED", r.status());
        assertEquals(11, r.clauses().size());
        // 6 finding soạn sẵn đều có quote khớp nguyên văn -> không bị grounding loại bỏ.
        assertEquals(6, r.findings().size());
        // Mỗi finding phải có căn cứ luật do code gắn (trừ loại không có trong bảng).
        assertTrue(r.findings().stream().anyMatch(f -> f.lawRef() != null));

        // Checklist 14 loại thiết yếu; hợp đồng mẫu thiếu 4 (hoàn cọc, hiện trạng, sửa chữa, tạm trú).
        assertEquals(14, r.checklist().size());
        long missing = r.checklist().stream().filter(c -> !c.present()).count();
        assertEquals(4, missing);
    }

    @Test
    void sampleScoreReflectsHighRisk() {
        AnalysisResponse r = provider.build();

        assertNotNull(r.safetyScore());
        assertTrue(r.safetyScore() < 50, "Hợp đồng mẫu nhiều rủi ro -> điểm phải thấp");
        assertTrue(r.verdictLabel().contains("Rủi ro cao"));
    }
}

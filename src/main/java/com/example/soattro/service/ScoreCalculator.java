package com.example.soattro.service;

import com.example.soattro.ai.RawFinding;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CHẤM ĐIỂM AN TOÀN (bước 3, code thuần) — quyết định thiết kế số 3 của pipeline.
 *
 * Công thức tất định (docs/rubric.md mục 5), AI KHÔNG tham gia:
 *   score = 100
 *           − Σ (mỗi LOẠI có finding: RED → W, chỉ YELLOW → round(0.4·W))
 *           − Σ (mỗi loại thiết yếu bị THIẾU: M)
 *           kẹp trong [0, 100]
 *
 * "Mỗi loại chỉ trừ 1 lần" (lấy finding nặng nhất của loại) — đo rủi ro theo CHỦ ĐỀ,
 * không theo số câu, để 3 câu mơ hồ về cùng tiền cọc không bị trừ 3 lần.
 */
@Component
public class ScoreCalculator {

    /**
     * @param groundedFindings findings ĐÃ qua verify grounding
     * @param missingEssential các loại thiết yếu bị thiếu (từ ChecklistBuilder)
     */
    public ScoreResult calculate(List<RawFinding> groundedFindings, List<ClauseType> missingEssential) {
        // 1) Gom finding theo loại, giữ mức rủi ro NẶNG NHẤT của mỗi loại.
        Map<ClauseType, RiskLevel> worstByType = new EnumMap<>(ClauseType.class);
        for (RawFinding f : groundedFindings) {
            worstByType.merge(f.clauseType(), f.riskLevel(),
                    (a, b) -> a.getSeverity() >= b.getSeverity() ? a : b);
        }

        int score = 100;
        int redCount = 0;
        int yellowCount = 0;
        for (Map.Entry<ClauseType, RiskLevel> e : worstByType.entrySet()) {
            int w = e.getKey().getWeight();
            if (e.getValue() == RiskLevel.RED) {
                score -= w;
                redCount++;
            } else if (e.getValue() == RiskLevel.YELLOW) {
                score -= Math.round(0.4f * w);
                yellowCount++;
            }
        }

        // 2) Trừ điểm cho từng loại thiết yếu bị thiếu.
        for (ClauseType missing : missingEssential) {
            score -= missing.getMissingPenalty();
        }

        score = Math.max(0, Math.min(100, score));
        return new ScoreResult(score, verdictLabel(score),
                summary(score, redCount, yellowCount, missingEssential.size()));
    }

    /** Nhãn màu theo điểm — public để GET /api/analyses/{id} tái tạo từ điểm đã lưu. */
    public String labelFor(int score) {
        if (score >= 80) {
            return "🟢 Khá an toàn";
        }
        if (score >= 50) {
            return "🟡 Cần làm rõ";
        }
        return "🔴 Rủi ro cao";
    }

    private String verdictLabel(int score) {
        return labelFor(score);
    }

    private String summary(int score, int red, int yellow, int missing) {
        if (score >= 80) {
            return "Hợp đồng khá chặt chẽ" + (yellow + missing > 0
                    ? ", chỉ cần làm rõ vài điểm nhỏ." : ".");
        }
        if (score >= 50) {
            StringBuilder sb = new StringBuilder("Có ");
            if (red > 0) {
                sb.append(red).append(" điều khoản rủi ro cao");
                if (yellow + missing > 0) {
                    sb.append(" và ");
                }
            }
            if (yellow + missing > 0) {
                sb.append(yellow + missing).append(" điểm cần làm rõ");
            }
            return sb.append(" — nên hỏi lại chủ trọ trước khi ký.").toString();
        }
        return "Nhiều điều khoản bất lợi nghiêm trọng (" + red
                + " rủi ro cao) — cân nhắc rất kỹ trước khi ký.";
    }
}

package com.example.soattro.service;

import com.example.soattro.ai.ExtractedClause;
import com.example.soattro.ai.RawFinding;
import com.example.soattro.dto.response.AnalysisResponse;
import com.example.soattro.dto.response.ChecklistItemDto;
import com.example.soattro.dto.response.ClauseDto;
import com.example.soattro.dto.response.FindingDto;
import com.example.soattro.entity.ClauseType;
import com.example.soattro.entity.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hợp đồng mẫu cho nút "Thử với hợp đồng mẫu" (chặng 7).
 *
 * Bài học từ finance app: người review phải thấy DEMO SỐNG chỉ trong 1 click,
 * kể cả khi máy chưa cấu hình GEMINI_API_KEY. Vì vậy demo KHÔNG gọi Gemini:
 * điều khoản + findings được soạn sẵn, nhưng vẫn chạy qua ĐÚNG code tất định
 * (grounding verify -> checklist -> chấm điểm) để kết quả trung thực, không bịa.
 */
@Component
public class SampleContractProvider {

    private final GroundingVerifier groundingVerifier;
    private final ChecklistBuilder checklistBuilder;
    private final ScoreCalculator scoreCalculator;
    private final LawReferences lawReferences;

    public SampleContractProvider(GroundingVerifier groundingVerifier,
                                  ChecklistBuilder checklistBuilder,
                                  ScoreCalculator scoreCalculator,
                                  LawReferences lawReferences) {
        this.groundingVerifier = groundingVerifier;
        this.checklistBuilder = checklistBuilder;
        this.scoreCalculator = scoreCalculator;
        this.lawReferences = lawReferences;
    }

    /** Điều khoản của hợp đồng mẫu (nguyên văn + phân loại), như output bước 1. */
    private List<ExtractedClause> sampleClauses() {
        return List.of(
                new ExtractedClause(ClauseType.THONG_TIN_CAC_BEN,
                        "Bên A (cho thuê): ông Nguyễn Văn A, CCCD 001099xxxxxx. "
                                + "Bên B (thuê): bà Trần Thị B, CCCD 001200xxxxxx."),
                new ExtractedClause(ClauseType.DOI_TUONG_THUE,
                        "Phòng trọ số 5, tầng 2, số 12 ngõ 34 phố Y, quận Z; diện tích 18m²."),
                new ExtractedClause(ClauseType.GIA_THUE_THANH_TOAN,
                        "Giá thuê: 2.500.000 đồng/tháng, thanh toán trước ngày 05 hàng tháng."),
                new ExtractedClause(ClauseType.THOI_HAN_THUE,
                        "Thời hạn thuê: 12 tháng kể từ ngày 01/08/2026."),
                new ExtractedClause(ClauseType.DAT_COC,
                        "Đặt cọc: 2 tháng tiền phòng, tương đương 5.000.000 đồng."),
                new ExtractedClause(ClauseType.GIA_DIEN,
                        "Giá điện: 4.000 đồng/kWh, thu theo chỉ số công tơ riêng."),
                new ExtractedClause(ClauseType.GIA_NUOC,
                        "Giá nước: 30.000 đồng/người/tháng."),
                new ExtractedClause(ClauseType.PHI_DICH_VU_KHAC,
                        "Phí dịch vụ chung (vệ sinh, gửi xe) thu theo thỏa thuận từng thời điểm."),
                new ExtractedClause(ClauseType.TANG_GIA,
                        "Bên A có quyền điều chỉnh giá thuê và các loại phí bất cứ lúc nào cho phù hợp thị trường."),
                new ExtractedClause(ClauseType.DON_PHUONG_CHAM_DUT,
                        "Bên A có quyền lấy lại phòng và không hoàn tiền cọc nếu Bên B chậm nộp tiền quá 05 ngày."),
                new ExtractedClause(ClauseType.NOI_QUY_SU_DUNG,
                        "Không tiếp khách sau 22h; giữ gìn vệ sinh chung; không nuôi thú lớn.")
        );
    }

    /** Findings soạn sẵn (quote lấy đúng nguyên văn để qua được grounding), như output bước 2. */
    private List<RawFinding> sampleFindings() {
        return List.of(
                new RawFinding(ClauseType.GIA_DIEN, RiskLevel.RED,
                        "Giá điện: 4.000 đồng/kWh",
                        "Giá điện cao hơn hẳn giá bán lẻ bậc thang của EVN; người thuê đúng ra được áp giá bậc thang.",
                        "Đề nghị áp đúng biểu giá điện EVN, hoặc ghi rõ mức giá và cách tính."),
                new RawFinding(ClauseType.GIA_NUOC, RiskLevel.YELLOW,
                        "Giá nước: 30.000 đồng/người/tháng",
                        "Thu khoán theo đầu người dễ bất lợi khi dùng ít; nên đối chiếu giá nước sạch địa phương.",
                        "Hỏi rõ căn cứ tính tiền nước và có công tơ riêng không."),
                new RawFinding(ClauseType.DAT_COC, RiskLevel.YELLOW,
                        "Đặt cọc: 2 tháng tiền phòng",
                        "Cọc 2 tháng cao hơn thông lệ phổ biến (1 tháng) — nên cân nhắc.",
                        "Thương lượng còn 1 tháng và ghi rõ điều kiện được hoàn cọc."),
                new RawFinding(ClauseType.PHI_DICH_VU_KHAC, RiskLevel.YELLOW,
                        "thu theo thỏa thuận từng thời điểm",
                        "Phí dịch vụ không ghi con số cụ thể, dễ bị nâng tùy tiện.",
                        "Yêu cầu ghi rõ từng khoản phí bằng số ngay trong hợp đồng."),
                new RawFinding(ClauseType.TANG_GIA, RiskLevel.RED,
                        "điều chỉnh giá thuê và các loại phí bất cứ lúc nào",
                        "Cho phép Bên A tăng giá đơn phương, không cần báo trước hay giới hạn mức tăng.",
                        "Đề nghị cố định giá trong suốt thời hạn, hoặc chỉ tăng khi gia hạn và báo trước 30 ngày."),
                new RawFinding(ClauseType.DON_PHUONG_CHAM_DUT, RiskLevel.RED,
                        "không hoàn tiền cọc nếu Bên B chậm nộp tiền quá 05 ngày",
                        "Mất trắng cọc chỉ vì chậm 5 ngày là điều khoản phạt bất cân xứng, bất lợi nặng cho người thuê.",
                        "Đề nghị bỏ điều khoản mất cọc; thay bằng nhắc nợ và thời gian khắc phục hợp lý.")
        );
    }

    /**
     * Dựng kết quả demo bằng chính pipeline bước 3 (code tất định).
     * id = null (không phải bản ghi trong DB) — frontend hiển thị như một lượt COMPLETED.
     */
    public AnalysisResponse build() {
        List<ExtractedClause> clauses = sampleClauses();
        String contractText = clauses.stream()
                .map(ExtractedClause::text)
                .collect(Collectors.joining("\n\n"));

        // Bước 3a: grounding (mọi quote đều khớp -> giữ nguyên, chứng minh code chạy thật).
        List<RawFinding> grounded = groundingVerifier.verify(contractText, sampleFindings());

        // Bước 3b: checklist thiếu.
        Set<ClauseType> presentTypes = clauses.stream()
                .map(ExtractedClause::clauseType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClauseType.class)));
        Map<ClauseType, Boolean> checklist = checklistBuilder.build(presentTypes);
        List<ClauseType> missingEssential = checklist.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .toList();

        // Bước 3c: chấm điểm.
        ScoreResult score = scoreCalculator.calculate(grounded, missingEssential);

        List<FindingDto> findingDtos = grounded.stream()
                .map(f -> new FindingDto(
                        f.clauseType().name(), f.clauseType().getLabel(), f.riskLevel().name(),
                        f.quote(), f.explanation(), f.suggestion(),
                        lawReferences.forType(f.clauseType())))
                .toList();
        List<ChecklistItemDto> checklistDtos = checklist.entrySet().stream()
                .map(e -> ChecklistItemDto.of(e.getKey(), e.getValue()))
                .toList();

        return new AnalysisResponse(
                null, "COMPLETED", score.score(), score.verdictLabel(), score.summary(),
                contractText, null, LocalDateTime.now(),
                clauses.stream().map(ClauseDto::from).toList(),
                findingDtos, checklistDtos);
    }
}

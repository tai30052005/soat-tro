package com.example.soattro.service;

import com.example.soattro.entity.ClauseType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bảng căn cứ pháp lý theo loại điều khoản — CODE gắn cho finding, KHÔNG để AI sinh.
 *
 * Chống rủi ro "trích luật sai" (SPEC mục 6): mọi số điều luật ở đây đã được tra cứu
 * và ghi nguồn trong docs/rubric.md (mục 6). Model chỉ đánh giá rủi ro; luật là của code.
 */
@Component
public class LawReferences {

    private final Map<ClauseType, String> refs = new EnumMap<>(ClauseType.class);

    public LawReferences() {
        refs.put(ClauseType.THONG_TIN_CAC_BEN, "Điều 163 Luật Nhà ở 2023; Điều 11 Nghị định 282/2025/NĐ-CP (giữ giấy tờ tùy thân)");
        refs.put(ClauseType.DOI_TUONG_THUE, "Điều 163 Luật Nhà ở 2023 (nội dung bắt buộc của hợp đồng)");
        refs.put(ClauseType.GIA_THUE_THANH_TOAN, "Điều 473, 481 Bộ luật Dân sự 2015");
        refs.put(ClauseType.TANG_GIA, "Điều 172 Luật Nhà ở 2023 (tăng giá bất hợp lý / không báo trước)");
        refs.put(ClauseType.DAT_COC, "Điều 328 Bộ luật Dân sự 2015 (đặt cọc)");
        refs.put(ClauseType.HOAN_COC, "Điều 328 Bộ luật Dân sự 2015 (hoàn cọc)");
        refs.put(ClauseType.GIA_DIEN, "Nghị định 133/2026/NĐ-CP; biểu giá điện QĐ 1279/QĐ-BCT");
        refs.put(ClauseType.GIA_NUOC, "Thông tư 44/2021/TT-BTC (khung giá nước sạch sinh hoạt)");
        refs.put(ClauseType.THOI_HAN_THUE, "Điều 474 Bộ luật Dân sự 2015 (thời hạn thuê)");
        refs.put(ClauseType.DON_PHUONG_CHAM_DUT, "Điều 172 Luật Nhà ở 2023 (đơn phương chấm dứt, báo trước ≥30 ngày)");
        refs.put(ClauseType.HIEN_TRANG_TAI_SAN, "Điều 477 Bộ luật Dân sự 2015 (nghĩa vụ bảo đảm tài sản thuê)");
        refs.put(ClauseType.SUA_CHUA_BAO_TRI, "Điều 477 Bộ luật Dân sự 2015 (nghĩa vụ sửa chữa của bên cho thuê)");
        refs.put(ClauseType.TAM_TRU_PHAP_LY, "Điều 27 Luật Cư trú 2020; Nghị định 282/2025/NĐ-CP");
        refs.put(ClauseType.NOI_QUY_SU_DUNG, "Điều 22 Hiến pháp 2013 (bất khả xâm phạm chỗ ở)");
        refs.put(ClauseType.PHAT_VI_PHAM, "Điều 418 Bộ luật Dân sự 2015 (thỏa thuận phạt vi phạm)");
        // PHI_DICH_VU_KHAC, OTHER: không gắn điều luật cụ thể (vấn đề minh bạch, không phải vi phạm luật riêng).
    }

    /** Trả căn cứ luật cho loại điều khoản (null nếu không có). */
    public String forType(ClauseType type) {
        return refs.get(type);
    }
}

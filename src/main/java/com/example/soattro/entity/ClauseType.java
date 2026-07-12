package com.example.soattro.entity;

/**
 * TAXONOMY CỐ ĐỊNH ~16 loại điều khoản hợp đồng thuê trọ + OTHER.
 *
 * Nguồn sự thật: docs/rubric.md. Mỗi loại mang sẵn 3 tham số chấm điểm (rubric mục 2):
 *   - essential: loại "thiết yếu" — vắng mặt sẽ vào checklist "hợp đồng THIẾU".
 *   - weight (W): điểm trừ khi loại này có finding RED (YELLOW trừ 40% × W).
 *   - missingPenalty (M): điểm trừ khi loại thiết yếu này bị thiếu.
 *
 * Đây là quyết định thiết kế số 1 của pipeline: AI bắt buộc phân loại vào danh sách
 * đóng này, còn trọng số điểm nằm ở CODE (không để AI chấm cảm tính) → điểm tái lập 100%.
 */
public enum ClauseType {
    THONG_TIN_CAC_BEN("Thông tin các bên", true, 4, 4),
    DOI_TUONG_THUE("Mô tả phòng thuê", true, 4, 4),
    GIA_THUE_THANH_TOAN("Giá thuê & thanh toán", true, 8, 8),
    TANG_GIA("Điều chỉnh giá thuê", true, 10, 7),
    DAT_COC("Đặt cọc", true, 10, 6),
    HOAN_COC("Hoàn cọc", true, 12, 10),
    GIA_DIEN("Giá điện", true, 10, 6),
    GIA_NUOC("Giá nước", true, 6, 4),
    PHI_DICH_VU_KHAC("Phí dịch vụ khác", true, 8, 5),
    THOI_HAN_THUE("Thời hạn thuê", true, 8, 6),
    DON_PHUONG_CHAM_DUT("Đơn phương chấm dứt", true, 12, 8),
    HIEN_TRANG_TAI_SAN("Hiện trạng tài sản", true, 6, 8),
    SUA_CHUA_BAO_TRI("Sửa chữa, bảo trì", true, 6, 4),
    TAM_TRU_PHAP_LY("Đăng ký tạm trú", true, 6, 5),
    NOI_QUY_SU_DUNG("Nội quy sử dụng", false, 6, 0),
    PHAT_VI_PHAM("Phạt vi phạm", false, 8, 0),
    /** Điều khoản không khớp loại nào — không chấm rủi ro, không tính checklist. */
    OTHER("Điều khoản khác", false, 0, 0);

    private final String label;
    private final boolean essential;
    private final int weight;
    private final int missingPenalty;

    ClauseType(String label, boolean essential, int weight, int missingPenalty) {
        this.label = label;
        this.essential = essential;
        this.weight = weight;
        this.missingPenalty = missingPenalty;
    }

    /** Tên hiển thị tiếng Việt cho frontend. */
    public String getLabel() {
        return label;
    }

    /** Thiết yếu = vắng mặt sẽ bị liệt kê ở checklist "hợp đồng này THIẾU". */
    public boolean isEssential() {
        return essential;
    }

    /** Trọng số W: điểm trừ khi loại này có finding RED. */
    public int getWeight() {
        return weight;
    }

    /** Điểm trừ M khi loại thiết yếu này bị thiếu. */
    public int getMissingPenalty() {
        return missingPenalty;
    }
}

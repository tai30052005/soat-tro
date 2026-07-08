package com.example.soattro.entity;

/**
 * TAXONOMY CỐ ĐỊNH ~16 loại điều khoản hợp đồng thuê trọ + OTHER.
 *
 * Nguồn sự thật: docs/rubric.md (mục 2). Đây là quyết định thiết kế số 1 của
 * pipeline: AI bắt buộc phân loại vào danh sách đóng này (structured output enum),
 * KHÔNG được tự nghĩ ra loại mới → checklist "thiếu gì" trở thành phép đếm
 * tất định của code thay vì ý kiến của AI.
 */
public enum ClauseType {
    THONG_TIN_CAC_BEN("Thông tin các bên"),
    DOI_TUONG_THUE("Mô tả phòng thuê"),
    GIA_THUE_THANH_TOAN("Giá thuê & thanh toán"),
    TANG_GIA("Điều chỉnh giá thuê"),
    DAT_COC("Đặt cọc"),
    HOAN_COC("Hoàn cọc"),
    GIA_DIEN("Giá điện"),
    GIA_NUOC("Giá nước"),
    PHI_DICH_VU_KHAC("Phí dịch vụ khác"),
    THOI_HAN_THUE("Thời hạn thuê"),
    DON_PHUONG_CHAM_DUT("Đơn phương chấm dứt"),
    HIEN_TRANG_TAI_SAN("Hiện trạng tài sản"),
    SUA_CHUA_BAO_TRI("Sửa chữa, bảo trì"),
    TAM_TRU_PHAP_LY("Đăng ký tạm trú"),
    NOI_QUY_SU_DUNG("Nội quy sử dụng"),
    PHAT_VI_PHAM("Phạt vi phạm"),
    /** Điều khoản không khớp loại nào — không chấm rủi ro, không tính checklist. */
    OTHER("Điều khoản khác");

    private final String label;

    ClauseType(String label) {
        this.label = label;
    }

    /** Tên hiển thị tiếng Việt cho frontend. */
    public String getLabel() {
        return label;
    }
}

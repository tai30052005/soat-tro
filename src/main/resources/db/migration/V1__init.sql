-- ============================================================
--  V1: Schema ban đầu của Soát Trọ (theo SPEC.md mục 5)
--  4 bảng: users, analyses, findings, checklist_items
-- ============================================================

-- Người dùng (đăng ký CHỈ để lưu lịch sử — phân tích ẩn danh vẫn được phép)
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- Một lượt phân tích hợp đồng.
-- user_id NULLABLE: cho phép người dùng ẩn danh (không đăng nhập) vẫn soát được.
-- Privacy-by-design: KHÔNG có cột lưu ảnh gốc — chỉ giữ text đã bóc tách.
CREATE TABLE analyses (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING/PROCESSING/COMPLETED/FAILED
    contract_text TEXT,                                     -- văn bản hợp đồng đã bóc từ ảnh/PDF
    safety_score  INTEGER,                                  -- điểm an toàn 0-100 (code tự tính, không phải AI chấm)
    error_message TEXT,                                     -- lý do khi status = FAILED (vd "ảnh không đủ rõ")
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- Kết quả phân tích từng điều khoản (finding).
-- quote BẮT BUỘC khớp nguyên văn hợp đồng (grounding) — code verify trước khi lưu.
CREATE TABLE findings (
    id          BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT      NOT NULL REFERENCES analyses (id) ON DELETE CASCADE,
    clause_type VARCHAR(50) NOT NULL,   -- loại điều khoản theo TAXONOMY cố định (chặng 2)
    risk_level  VARCHAR(10) NOT NULL,   -- RED / YELLOW / GREEN
    quote       TEXT        NOT NULL,   -- trích dẫn nguyên văn từ hợp đồng
    explanation TEXT        NOT NULL,   -- giải thích dễ hiểu cho người thuê
    suggestion  TEXT,                   -- gợi ý câu hỏi / cách thương lượng với chủ trọ
    law_ref     TEXT                    -- căn cứ pháp lý (điền từ rubric, sau này từ RAG)
);

-- Checklist "hợp đồng này THIẾU gì": mỗi loại điều khoản trong taxonomy
-- có mặt (present = true) hay vắng (false) — code kết luận tất định.
CREATE TABLE checklist_items (
    id          BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT      NOT NULL REFERENCES analyses (id) ON DELETE CASCADE,
    clause_type VARCHAR(50) NOT NULL,
    present     BOOLEAN     NOT NULL
);

-- Index cho các truy vấn chính: lịch sử theo user, nạp kết quả theo analysis.
CREATE INDEX idx_analyses_user_id        ON analyses (user_id);
CREATE INDEX idx_findings_analysis_id    ON findings (analysis_id);
CREATE INDEX idx_checklist_analysis_id   ON checklist_items (analysis_id);

# Soát Trọ — AI soát hợp đồng thuê trọ

> File này là bản kế hoạch đầy đủ, được chốt từ buổi brainstorm ngày 2026-07-08.
> **Session mới: đọc kỹ file này trước khi làm bất cứ điều gì.**

## 0. Bối cảnh & quy ước làm việc (QUAN TRỌNG)

- Người làm: **Võ Minh Tài** — sinh viên, đã hoàn thành 2 project cho CV intern backend/fullstack:
  1. Personal Finance App (cá nhân, full-stack Spring Boot + React, có AI Gemini, đã deploy) — repo `tai30052005/personal-finance-api`, source tại `G:\duanrieng\personal-financal-app`.
  2. Hamora Booking (project nhóm ở FPT).
- Đây là **project thứ 3 cho CV**: sản phẩm AI-first giải quyết vấn đề xã hội thật, khác biệt kỹ thuật so với 2 cái trước.
- **Quy ước bắt buộc (mang từ project trước sang):**
  - Commit chỉ đứng tên **Võ Minh Tài** — TUYỆT ĐỐI KHÔNG thêm `Co-Authored-By: Claude`.
  - Tiền tệ dùng `BigDecimal`, không bao giờ `double`/`float`.
  - Trả lời bằng **tiếng Việt**, giải thích từng bước (user muốn hiểu code, không chỉ có code). Build từng chặng, xác nhận rồi mới đi tiếp.
  - Gemini: dùng model **`gemini-2.5-flash`** (2.0-flash bị limit 0 trên free tier). API key qua env `GEMINI_API_KEY`.
  - User push code bằng GitHub Desktop, không có gh CLI.
  - Deploy 0đ: Vercel (frontend) + Render (backend Docker) + Neon (Postgres) — đã có kinh nghiệm từ project trước.

## 1. Sản phẩm trong một câu

Chụp/upload hợp đồng thuê trọ → 30 giây sau biết: điều khoản nào rủi ro (tô màu 🔴🟡🟢), hợp đồng THIẾU điều khoản gì, và cần hỏi lại chủ trọ câu gì — **người dùng không cần gõ một chữ nào** (zero-prompt).

Tên ứng viên: Soát Trọ / Trọ An Tâm / Ký Không Lo (chưa chốt).

## 2. Vì sao làm cái này (để kể khi phỏng vấn)

- Gap thị trường: chưa có tool tiếng Việt nào soát hợp đồng thuê trọ tử tế. ChatGPT/Gemini chat không thay được vì: (1) người thường không biết hỏi gì, (2) chat không tự soát cái *bị thiếu*, (3) không có kiến thức luật VN + chiêu lừa phổ biến đóng gói sẵn, (4) kết quả chat là văn xuôi, không phải bảng kết quả có cấu trúc.
- Câu chuyện thật: chính user là sinh viên thuê trọ, từng ký hợp đồng mà không hiểu.
- Nỗi đau thật cần đóng gói vào rubric: cọc không hoàn (điều khoản hoàn cọc mơ hồ/thiếu); giá điện nước "tự quy định" vượt quy định EVN; tăng giá tùy tiện; đơn phương chấm dứt bất cân xứng; thiếu biên bản hiện trạng tài sản; không cam kết đăng ký tạm trú; phí phát sinh mơ hồ (quản lý/rác/xe không ghi số).

## 3. Luồng người dùng

1. Trang chủ → "Soát hợp đồng" → upload ảnh (nhiều trang) hoặc PDF. **Không bắt đăng nhập** (đăng ký chỉ để lưu lịch sử).
2. Màn hình chờ ~20–30s có tiến trình ("Đang đọc… Đang đối chiếu 25 điểm kiểm tra…").
3. Trang kết quả:
   - ① Điểm an toàn tổng (VD 62/100) + nhận định 1 câu.
   - ② Văn bản hợp đồng hiển thị lại, từng điều khoản tô màu 🔴 rủi ro cao / 🟡 cần làm rõ / 🟢 ổn; bấm vào → giải thích dễ hiểu + căn cứ.
   - ③ Checklist "hợp đồng này THIẾU": ✗ hoàn cọc ✗ hiện trạng tài sản ✗ tạm trú…
   - ④ "5 câu cầm đi hỏi chủ trọ" — copy/in được.
4. Nút **"Thử với hợp đồng mẫu"** ngay trang chủ (bài học từ finance app: reviewer phải thấy demo sống trong 1 click).

## 4. Kiến trúc AI — pipeline 3 bước (điểm ăn tiền kỹ thuật)

```
Ảnh/PDF → [1] Gemini Vision: bóc tách điều khoản (structured output JSON:
              mỗi điều khoản = nguyên văn + phân loại vào TAXONOMY cố định ~15 loại)
        → [2] Gemini: phân tích từng điều khoản theo RUBRIC thiết kế sẵn
              (findings BẮT BUỘC kèm trích dẫn nguyên văn)
        → [3] CODE TỰ VIẾT (không phải AI):
              - verify grounding: finding không có quote khớp văn bản gốc → LOẠI (chống hallucination)
              - checklist thiếu = taxonomy slot trống → code kết luận tất định
              - điểm an toàn = công thức có trọng số tự định nghĩa, KHÔNG cho AI chấm cảm tính
```

3 quyết định thiết kế = 3 câu trả lời phỏng vấn: taxonomy cố định, grounding bắt buộc, điểm số tất định.

Chặng sau: **RAG mini** — corpus luật tự kiểm soát (Luật Nhà ở 2023, BLDS 2015 phần thuê tài sản + đặt cọc Đ328, quy định giá điện cho người thuê) → Gemini embedding (free) → trích điều luật cho từng finding. **Số điều luật cụ thể PHẢI tra và xác minh khi build rubric, không phỏng đoán.**

## 5. Stack & kiến trúc hệ thống

- Spring Boot 3 / Java 17 + React (Vite) + PostgreSQL (Neon) + Flyway + JWT auth (copy pattern từ finance app) + Docker Compose + GitHub Actions CI. Repo mới: thư mục này (`G:\duanrieng\soat-tro`).
- Cái MỚI so với finance app (giá trị học tập): multipart file upload; pipeline **@Async** với trạng thái PENDING/PROCESSING/COMPLETED/FAILED; frontend poll `GET /api/analyses/{id}` (hoặc SSE); prompt multimodal; JSON schema output; grounding verify.
- **Privacy-by-design:** KHÔNG lưu ảnh gốc lâu dài — xử lý xong giữ text đã bóc + kết quả, xóa ảnh. (Điểm cộng khi kể chuyện.)

### Database phác thảo (Flyway V1)
- `users` (như finance app)
- `analyses`: id, user_id (nullable — cho phép ẩn danh), status, contract_text, safety_score, created_at
- `findings`: analysis_id, clause_type, risk_level (RED/YELLOW/GREEN), quote, explanation, suggestion, law_ref
- `checklist_items`: analysis_id, clause_type, present (boolean)

### API phác thảo
`POST /api/analyses` (multipart) · `GET /api/analyses/{id}` · `GET /api/analyses` (lịch sử, cần auth) · `POST /api/analyses/sample` (demo mẫu) · `/api/auth/**` như cũ.

## 6. Rủi ro & cách né

| Rủi ro | Cách né |
|---|---|
| AI đọc sai/bịa | Grounding bắt buộc + hiển thị nguyên văn cho người dùng tự đối chiếu |
| Ảnh mờ / hợp đồng viết tay | Trả lời đàng hoàng "ảnh không đủ rõ"; viết tay để giai đoạn sau |
| Trách nhiệm pháp lý | Định vị "công cụ tham khảo, không phải tư vấn pháp lý" — disclaimer mọi trang kết quả |
| Trích dẫn luật sai | Rubric xây thủ công có nguồn đã tra; RAG chỉ trích từ corpus mình kiểm soát |
| Gemini rate limit free tier | Queue + retry; mỗi phân tích chỉ 2–3 call |
| Scope phình | Khóa cứng: CHỈ hợp đồng thuê trọ. HĐLĐ/bảo hiểm = Possible Improvements |

## 7. Lộ trình 8 chặng

1. Skeleton: Spring Boot + React + Docker + Flyway V1 (nhanh, pattern đã thạo).
2. **Rubric & taxonomy** ← chặng quan trọng nhất, làm trước khi code AI: ~15 loại điều khoản, rule đỏ/vàng cho từng loại, tra + ghi nguồn luật từng rule (file `docs/rubric.md`).
3. Upload + Gemini Vision đọc hợp đồng → structured output (điều khoản đã phân loại).
4. Engine phân tích + verify grounding + tính điểm — trái tim app, viết unit test kỹ.
5. Trang kết quả — highlight màu, checklist, câu hỏi gợi ý (đầu tư UX như garden hero).
6. Async + trạng thái + lịch sử + auth.
7. Hợp đồng mẫu demo + tests + GitHub Actions CI.
8. Deploy (Vercel + Render + Neon) + README + screenshot + seed demo.

**Chặng đầu tiên khi bắt đầu session mới: chặng 2 (rubric) hoặc chặng 1 (skeleton) — user chọn.**

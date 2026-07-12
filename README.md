# 🏠 Soát Trọ — AI soát hợp đồng thuê trọ

[![CI](https://github.com/tai30052005/soat-tro/actions/workflows/ci.yml/badge.svg)](https://github.com/tai30052005/soat-tro/actions/workflows/ci.yml)

> Chụp/upload hợp đồng thuê trọ → 30 giây sau biết: điều khoản nào rủi ro (🔴🟡🟢),
> hợp đồng **thiếu** điều khoản gì, và cần hỏi lại chủ trọ câu gì — không cần gõ một chữ nào.
>
> Chưa có hợp đồng? Bấm **“Thử với hợp đồng mẫu”** ở trang chủ để xem kết quả demo ngay (không cần API key).

⚠️ Soát Trọ là **công cụ tham khảo, không phải tư vấn pháp lý**.

<!-- 📸 Ảnh minh hoạ: chạy app → mở "Thử với hợp đồng mẫu" → chụp trang kết quả,
     lưu vào docs/screenshot-result.png rồi bỏ comment dòng dưới. -->
<!-- ![Trang kết quả Soát Trọ](docs/screenshot-result.png) -->

## Stack

- **Backend**: Spring Boot 3 (Java 17), PostgreSQL, Flyway, JWT, Springdoc OpenAPI
- **Frontend**: React 19 + Vite
- **AI**: Google Gemini (pipeline 3 bước: bóc tách → phân tích theo rubric → verify grounding + chấm điểm tất định bằng code)
- **Hạ tầng**: Docker Compose · GitHub Actions CI · Vercel + Render + Neon (deploy 0đ)

## Kiến trúc AI — pipeline 3 bước

```
Ảnh/PDF ─▶ [1] Gemini Vision: bóc tách điều khoản, phân loại vào TAXONOMY cố định (~16 loại)
        ─▶ [2] Gemini: phân tích từng điều khoản theo RUBRIC, mỗi finding BẮT BUỘC kèm trích dẫn nguyên văn
        ─▶ [3] CODE tự viết (KHÔNG phải AI):
               • verify grounding : finding không khớp nguyên văn hợp đồng → LOẠI (chống bịa)
               • checklist thiếu  : taxonomy slot trống → kết luận tất định
               • điểm an toàn      : công thức trọng số cố định (docs/rubric.md), không cho AI chấm cảm tính
```

**3 quyết định thiết kế = 3 điểm ăn tiền khi phỏng vấn:**

1. **Taxonomy đóng (enum cố định)** — Gemini chỉ được chọn trong danh sách loại điều khoản, không tự bịa loại → checklist "thiếu gì" trở thành bài toán tất định.
2. **Grounding bắt buộc** — mọi nhận định phải trích đúng nguyên văn; code đối chiếu lại với văn bản gốc, không khớp thì loại → chống ảo giác (hallucination).
3. **Chấm điểm tất định** — điểm 0–100 do code tính theo công thức trọng số, AI không tham gia → cùng hợp đồng luôn ra cùng điểm, giải thích được từng điểm trừ.

Căn cứ pháp lý (`law_ref`) do **code** gắn từ bảng tra cứu đã kiểm chứng (`docs/rubric.md`), không để AI tự sinh.

## Chạy local

```bash
# Cách 1: Docker Compose (đủ cả 3 service)
docker compose up --build
# Frontend: http://localhost:3001 — API: http://localhost:8081 — Postgres: localhost:5433

# Cách 2: dev từng phần
docker compose up -d db                    # chỉ Postgres
DB_PORT=5433 mvn spring-boot:run           # backend tại :8080
cd frontend && npm install && npm run dev  # frontend tại :5173 (proxy /api -> :8080)
```

## Test

```bash
mvn test          # backend: H2 in-memory, không cần Postgres
cd frontend && npm run build   # frontend: kiểm tra build
```

## Deploy (0đ)

| Thành phần | Nền tảng | Cấu hình |
|---|---|---|
| Database | **Neon** | Tạo project Postgres, lấy host/db/user/password |
| Backend  | **Render** | Blueprint `render.yaml` (build từ `Dockerfile`); điền `DB_*`, `GEMINI_API_KEY`, `CORS_ALLOWED_ORIGINS` |
| Frontend | **Vercel** | Import thư mục `frontend/` (config `vercel.json`); đặt env `VITE_API_URL` = URL backend Render |

Các bước:

1. **Neon**: tạo database, copy thông tin kết nối.
2. **Render**: *New + → Blueprint* trỏ vào repo. Điền biến môi trường `DB_HOST/DB_NAME/DB_USER/DB_PASSWORD` (từ Neon), `GEMINI_API_KEY`, và `CORS_ALLOWED_ORIGINS` = domain Vercel. `JWT_SECRET` để Render tự sinh.
3. **Vercel**: import `frontend/`, đặt `VITE_API_URL` = URL service Render (vd `https://soat-tro-api.onrender.com`), deploy.

> Bí mật (khóa Gemini, mật khẩu DB) chỉ đặt trong dashboard từng nền tảng — **không commit vào repo**.

## Tiến độ (lộ trình 8 chặng — xem SPEC.md)

- [x] 1. Skeleton: Spring Boot + React + Docker + Flyway V1
- [x] 2. Rubric & taxonomy điều khoản (docs/rubric.md)
- [x] 3. Upload + Gemini Vision đọc hợp đồng → phân loại điều khoản (structured output)
- [x] 4. Engine phân tích + verify grounding + tính điểm
- [x] 5. Trang kết quả (highlight màu, checklist, câu hỏi gợi ý)
- [x] 6. Async + trạng thái + lịch sử + auth
- [x] 7. Hợp đồng mẫu demo + tests + CI
- [x] 8. Deploy (Vercel + Render + Neon) + README + screenshot

# 🏠 Soát Trọ — AI soát hợp đồng thuê trọ

> Chụp/upload hợp đồng thuê trọ → 30 giây sau biết: điều khoản nào rủi ro (🔴🟡🟢),
> hợp đồng **thiếu** điều khoản gì, và cần hỏi lại chủ trọ câu gì — không cần gõ một chữ nào.

⚠️ Soát Trọ là **công cụ tham khảo, không phải tư vấn pháp lý**.

## Stack

- **Backend**: Spring Boot 3 (Java 17), PostgreSQL, Flyway, JWT, Springdoc OpenAPI
- **Frontend**: React 19 + Vite
- **AI**: Google Gemini (pipeline 3 bước: bóc tách → phân tích theo rubric → verify grounding + chấm điểm tất định bằng code)
- **Hạ tầng**: Docker Compose · GitHub Actions CI · Vercel + Render + Neon (deploy 0đ)

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
mvn test
```

## Tiến độ (lộ trình 8 chặng — xem SPEC.md)

- [x] 1. Skeleton: Spring Boot + React + Docker + Flyway V1
- [ ] 2. Rubric & taxonomy điều khoản (docs/rubric.md)
- [ ] 3. Upload + Gemini Vision đọc hợp đồng
- [ ] 4. Engine phân tích + verify grounding + tính điểm
- [ ] 5. Trang kết quả (highlight màu, checklist, câu hỏi gợi ý)
- [ ] 6. Async + trạng thái + lịch sử + auth
- [ ] 7. Hợp đồng mẫu demo + tests + CI
- [ ] 8. Deploy + README + screenshot

# Rubric & Taxonomy — Soát Trọ

> Tài liệu thiết kế cho pipeline AI (chặng 3–4). Mọi căn cứ pháp lý trong file này
> đã được **tra cứu và xác minh ngày 08/07/2026** (danh sách nguồn ở cuối file).
> Đây là "nguồn sự thật" duy nhất: prompt Gemini, engine phân tích và công thức
> điểm đều phải bám theo file này, KHÔNG để AI tự bịa rule hay tự trích luật.

## 1. Cách hệ thống dùng file này

```
[Bước 1] Gemini Vision bóc tách hợp đồng → mỗi điều khoản GẮN 1 MÃ trong taxonomy (mục 2).
         Không khớp mã nào → OTHER (không chấm rủi ro, không tính checklist).
[Bước 2] Gemini phân tích từng điều khoản THEO RULE của mã đó (mục 3) → findings
         (mỗi finding bắt buộc kèm quote nguyên văn).
[Bước 3] CODE (không phải AI):
         - verify quote khớp văn bản gốc, sai thì LOẠI finding;
         - checklist thiếu  = mã thiết yếu không xuất hiện (mục 4);
         - điểm an toàn     = công thức tất định (mục 5).
```

## 2. Taxonomy — 16 loại điều khoản (CỐ ĐỊNH)

Mã enum dùng thẳng cho cột `findings.clause_type` và `checklist_items.clause_type` (DB V1).

| # | Mã | Tên | Thiết yếu? | Trọng số W | Phạt thiếu M |
|---|----|-----|:---:|:---:|:---:|
| 1 | `THONG_TIN_CAC_BEN` | Thông tin bên thuê / bên cho thuê | ✔ | 4 | 4 |
| 2 | `DOI_TUONG_THUE` | Mô tả phòng trọ (địa chỉ, diện tích, tình trạng) | ✔ | 4 | 4 |
| 3 | `GIA_THUE_THANH_TOAN` | Giá thuê + kỳ hạn & phương thức thanh toán | ✔ | 8 | 8 |
| 4 | `TANG_GIA` | Điều chỉnh giá thuê | ✔ | 10 | 7 |
| 5 | `DAT_COC` | Đặt cọc (mức, mục đích) | ✔ | 10 | 6 |
| 6 | `HOAN_COC` | Điều kiện & thời hạn hoàn cọc | ✔ | 12 | 10 |
| 7 | `GIA_DIEN` | Giá điện | ✔ | 10 | 6 |
| 8 | `GIA_NUOC` | Giá nước | ✔ | 6 | 4 |
| 9 | `PHI_DICH_VU_KHAC` | Phí khác (rác, xe, quản lý, internet, thang máy…) | ✔ | 8 | 5 |
| 10 | `THOI_HAN_THUE` | Thời hạn thuê & gia hạn | ✔ | 8 | 6 |
| 11 | `DON_PHUONG_CHAM_DUT` | Đơn phương chấm dứt & báo trước | ✔ | 12 | 8 |
| 12 | `HIEN_TRANG_TAI_SAN` | Biên bản hiện trạng / danh mục tài sản bàn giao | ✔ | 6 | 8 |
| 13 | `SUA_CHUA_BAO_TRI` | Trách nhiệm sửa chữa, bảo trì | ✔ | 6 | 4 |
| 14 | `TAM_TRU_PHAP_LY` | Đăng ký tạm trú | ✔ | 6 | 5 |
| 15 | `NOI_QUY_SU_DUNG` | Nội quy sử dụng (giờ giấc, khách, vào phòng…) | ✖ | 6 | 0 |
| 16 | `PHAT_VI_PHAM` | Phạt vi phạm / bồi thường | ✖ | 8 | 0 |

- **Thiết yếu ✔** = vắng mặt sẽ bị liệt kê ở checklist "hợp đồng này THIẾU" và trừ điểm M.
- **W** = điểm trừ khi có finding 🔴 RED ở loại đó (🟡 YELLOW trừ 40% × W, làm tròn).

## 3. Rubric từng loại điều khoản

Quy ước chung cho mọi loại:
- 🔴 RED = điều khoản gây thiệt hại rõ ràng / trái quy định pháp luật.
- 🟡 YELLOW = mơ hồ, thiếu chi tiết, cần hỏi lại chủ trọ trước khi ký.
- 🟢 GREEN = rõ ràng, cân bằng, đúng luật.
- Mỗi rule kèm **[căn cứ]**; giải thích cho người dùng phải diễn đạt dễ hiểu, KHÔNG chỉ trích số điều luật.

### 3.1 `THONG_TIN_CAC_BEN`
- 🔴 Không ghi họ tên / số giấy tờ của bên cho thuê, hoặc người ký không phải chủ sở hữu và không có giấy ủy quyền. *[Đ163 Luật Nhà ở 2023: hợp đồng phải ghi họ tên, địa chỉ các bên]*
- 🔴 Yêu cầu người thuê **nộp/để lại bản gốc CCCD/thẻ căn cước** cho chủ trọ. *[chủ trọ không có thẩm quyền giữ giấy tờ tùy thân; NĐ 282/2025/NĐ-CP Đ11 (hiệu lực 15/12/2025, thay NĐ 144/2021): chiếm đoạt/sử dụng trái phép thẻ căn cước của người khác phạt 2–4 triệu, nhận cầm cố thẻ căn cước phạt 8–10 triệu]*
- 🟡 Thiếu địa chỉ / số điện thoại liên hệ của bên cho thuê.
- 🟢 Đủ họ tên, giấy tờ, địa chỉ hai bên; chỉ dùng **bản sao** CCCD của người thuê.
- Câu hỏi gợi ý: "Anh/chị có phải chủ sở hữu nhà không? Nếu không, cho em xem giấy ủy quyền được không?"

### 3.2 `DOI_TUONG_THUE`
- 🔴 Không ghi địa chỉ cụ thể của phòng thuê (số phòng, số nhà).
- 🟡 Không ghi diện tích hoặc tình trạng phòng lúc bàn giao.
- 🟢 Ghi rõ số phòng, địa chỉ, diện tích, trang bị kèm theo. *[Đ163 Luật Nhà ở 2023: mô tả đặc điểm nhà ở là nội dung bắt buộc của hợp đồng]*
- Câu hỏi gợi ý: "Phòng em thuê là phòng số mấy, diện tích bao nhiêu — mình ghi rõ vào hợp đồng nhé?"

### 3.3 `GIA_THUE_THANH_TOAN`
- 🔴 Không ghi số tiền thuê cụ thể, hoặc ghi "theo thông báo của chủ nhà". *[Đ473 BLDS 2015: giá thuê do các bên thỏa thuận — không thỏa thuận rõ thì tính theo giá thị trường, dễ tranh chấp]*
- 🟡 Không ghi kỳ thanh toán (đầu/cuối tháng) hoặc phương thức (tiền mặt/chuyển khoản). *[Đ481 BLDS 2015: không thỏa thuận thời hạn trả tiền → xác định theo tập quán, mơ hồ]*
- 🟡 Yêu cầu đóng trước nhiều hơn 1 tháng tiền thuê (chưa kể cọc).
- 🟢 Ghi rõ số tiền, ngày đóng, phương thức, có biên nhận/chuyển khoản.
- Câu hỏi gợi ý: "Tiền thuê đóng ngày nào hằng tháng, có biên nhận hoặc chuyển khoản để lưu lại không?"

### 3.4 `TANG_GIA`
- 🔴 Chủ nhà được tăng giá **bất kỳ lúc nào / không cần báo trước / trong thời hạn hợp đồng**. *[Đ172 k3 Luật Nhà ở 2023: bên thuê được đơn phương chấm dứt nếu bên cho thuê tăng giá bất hợp lý hoặc tăng không báo trước theo thỏa thuận — hợp đồng cho tăng tùy tiện là đẩy toàn bộ rủi ro sang người thuê]*
- 🟡 Có nói được điều chỉnh giá nhưng không ghi rõ mức trần (%) và thời điểm (vd chỉ khi gia hạn).
- 🟢 Cam kết giữ giá trong suốt thời hạn hợp đồng; nếu điều chỉnh khi gia hạn thì ghi % tối đa và báo trước ≥30 ngày.
- Câu hỏi gợi ý: "Trong thời hạn hợp đồng giá thuê có cố định không? Khi gia hạn thì tăng tối đa bao nhiêu %?"

### 3.5 `DAT_COC`
- 🔴 Điều khoản kiểu "tiền cọc thuộc về bên cho thuê trong mọi trường hợp chấm dứt hợp đồng". *[Đ328 k2 BLDS 2015: nếu bên NHẬN cọc từ chối thực hiện hợp đồng thì phải trả lại cọc + một khoản tương đương — cọc không thể mặc nhiên mất về một phía]*
- 🟡 Cọc từ 2 tháng tiền thuê trở lên (phổ biến thị trường là 1 tháng; luật không đặt trần nên chỉ cảnh báo).
- 🟡 Không ghi rõ cọc để bảo đảm nghĩa vụ gì (trả phòng, bù hư hỏng, nợ phí…).
- 🟢 Cọc ≤1 tháng, ghi rõ mục đích và số tiền bằng chữ, có biên nhận.
- Câu hỏi gợi ý: "Tiền cọc này bảo đảm cho những khoản gì? Mình ghi rõ vào hợp đồng được không?"

### 3.6 `HOAN_COC` ← nỗi đau số 1 của người thuê trọ
- 🔴 Điều kiện hoàn cọc mơ hồ: "hoàn cọc tùy chủ nhà xem xét", "trừ cọc các chi phí phát sinh" không giới hạn mức, không nêu căn cứ.
- 🔴 Người thuê báo trước đúng thỏa thuận vẫn mất toàn bộ cọc.
- 🟡 Có cam kết hoàn cọc nhưng không ghi **số ngày** hoàn (vd "hoàn trong 7 ngày sau khi trả phòng").
- 🟡 Khấu trừ cọc không gắn với biên bản hiện trạng/danh mục tài sản (dễ bị "vẽ" hư hỏng).
- 🟢 Ghi rõ: hoàn đủ cọc trong X ngày sau khi trả phòng, chỉ trừ các khoản có căn cứ (chốt điện nước, hư hỏng đối chiếu biên bản bàn giao). *[Đ328 BLDS 2015]*
- Câu hỏi gợi ý: "Khi em trả phòng đúng hẹn, cọc được hoàn trong bao nhiêu ngày? Những khoản nào sẽ bị trừ và căn cứ vào đâu?"

### 3.7 `GIA_DIEN`
- 🔴 Giá điện **cao hơn ~3.800 đ/kWh** (bậc cao nhất 3.460 đ/kWh của biểu giá bán lẻ điện sinh hoạt + VAT) hoặc ghi "giá điện do chủ nhà quy định". *[biểu giá bán lẻ điện sinh hoạt theo QĐ 1279/QĐ-BCT (09/5/2025): 6 bậc 1.984–3.460 đ/kWh CHƯA gồm VAT; NĐ 133/2026/NĐ-CP (hiệu lực 25/5/2026): thu tiền điện của người thuê cao hơn giá quy định bị phạt 20–30 triệu + buộc hoàn trả]*
- 🟡 Giá điện 3.000–3.800 đ/kWh nhưng không nêu căn cứ; hoặc không ghi chỉ số công tơ tại thời điểm nhận phòng.
- 🟡 Gộp chi phí điện khu vực chung (đèn hành lang, thang máy, camera) vào đơn giá điện thay vì tách thành phí dịch vụ riêng. *[NĐ 133/2026: chi phí vận hành phải tách riêng, không được cộng vào giá điện]*
- 🟢 Giá điện ≤ giá bậc cao nhất (sau VAT), ghi rõ đơn giá + chỉ số công tơ đầu kỳ; tốt nhất: thuê ≥12 tháng có tạm trú thì hỏi ký hợp đồng điện trực tiếp với điện lực.
- Câu hỏi gợi ý: "Giá điện anh/chị thu dựa trên căn cứ nào? Chỉ số công tơ hiện tại là bao nhiêu — mình ghi vào hợp đồng nhé?"

### 3.8 `GIA_NUOC`
- 🔴 Giá nước **trên 18.000 đ/m³** (vượt trần khung giá nước sạch sinh hoạt toàn quốc) hoặc "do chủ nhà quy định". *[TT 44/2021/TT-BTC Đ3: khung giá nước sạch sinh hoạt 2.000–18.000 đ/m³ (đã gồm VAT), giá cụ thể do UBND tỉnh quyết định]*
- 🟡 Thu khoán theo đầu người (vd 100k/người) mà không nêu định mức m³ tương ứng.
- 🟢 Thu theo đồng hồ riêng với đơn giá nằm trong khung, ghi chỉ số đầu kỳ.
- Câu hỏi gợi ý: "Nước tính theo đồng hồ hay khoán đầu người? Đơn giá bao nhiêu một khối?"

### 3.9 `PHI_DICH_VU_KHAC`
- 🔴 Điều khoản mở kiểu "các khoản phí phát sinh khác theo thông báo của chủ nhà" — cho phép đẻ thêm phí không giới hạn.
- 🟡 Có liệt kê loại phí (rác, xe, wifi, quản lý) nhưng không ghi số tiền cụ thể từng khoản.
- 🟢 Liệt kê đầy đủ từng khoản phí + số tiền + cam kết không phát sinh khoản mới nếu không thỏa thuận lại.
- Câu hỏi gợi ý: "Ngoài tiền phòng, điện, nước thì mỗi tháng em phải đóng đúng những khoản nào, mỗi khoản bao nhiêu?"

### 3.10 `THOI_HAN_THUE`
- 🔴 Không ghi thời hạn thuê **và** cho phép chủ nhà lấy lại phòng bất kỳ lúc nào.
- 🟡 Không ghi thời hạn thuê (mơ hồ khi tranh chấp). *[Đ474 BLDS 2015: không thỏa thuận thời hạn thì xác định theo mục đích thuê — rất khó chứng minh]*
- 🟡 Không nói gì về việc gia hạn / ưu tiên tái ký.
- 🟢 Ghi rõ ngày bắt đầu – kết thúc; điều kiện gia hạn.
- Câu hỏi gợi ý: "Hết hạn hợp đồng thì em có được ưu tiên ký tiếp không, báo trước bao lâu?"

### 3.11 `DON_PHUONG_CHAM_DUT`
- 🔴 Chủ nhà được chấm dứt hợp đồng **không cần lý do / không cần báo trước**, hoặc quyền chấm dứt chỉ một phía (người thuê nghỉ thì mất cọc, chủ đuổi thì không đền gì). *[Đ172 Luật Nhà ở 2023: bên cho thuê chỉ được đơn phương chấm dứt trong các trường hợp luật định — k2: không trả đủ tiền thuê từ 03 tháng trở lên không có lý do đã thỏa thuận, dùng sai mục đích, tự ý đục phá/cơi nới, tự ý cho thuê lại, gây mất trật tự vệ sinh đã lập biên bản đến lần thứ 3…; k4: phải thông báo trước ít nhất 30 ngày trừ thỏa thuận khác]*
- 🟡 Thời hạn báo trước dưới 30 ngày, hoặc không liệt kê trường hợp cụ thể được chấm dứt.
- 🟢 Quyền chấm dứt đối xứng hai bên, nêu trường hợp cụ thể, báo trước ≥30 ngày, nêu rõ hệ quả cọc từng trường hợp.
- Câu hỏi gợi ý: "Nếu anh/chị cần lấy lại phòng trước hạn thì em được báo trước bao lâu và cọc xử lý thế nào?"

### 3.12 `HIEN_TRANG_TAI_SAN`
- 🔴 Quy toàn bộ hư hỏng cho người thuê **kể cả hao mòn tự nhiên** ("mọi hư hỏng trong phòng người thuê phải đền"). *[Đ477 BLDS 2015: bên cho thuê phải sửa chữa hư hỏng, khuyết tật của tài sản thuê, trừ hư hỏng nhỏ theo tập quán bên thuê tự sửa]*
- 🟡 Có nhắc nội thất kèm theo nhưng không có danh mục/biên bản bàn giao ghi tình trạng từng món.
- 🟢 Có biên bản bàn giao (hoặc phụ lục) liệt kê tài sản + tình trạng + ảnh chụp, làm căn cứ hoàn cọc.
- Câu hỏi gợi ý: "Mình lập biên bản bàn giao ghi tình trạng đồ đạc và chụp ảnh lúc nhận phòng nhé?"

### 3.13 `SUA_CHUA_BAO_TRI`
- 🔴 Đẩy toàn bộ chi phí sửa chữa (kể cả hư hỏng lớn, kết cấu, thấm dột, điện nước âm tường) cho người thuê. *[Đ477 BLDS 2015 — nghĩa vụ sửa chữa hư hỏng thuộc bên cho thuê; bên thuê đã báo mà chủ không sửa thì được tự sửa với chi phí hợp lý và yêu cầu hoàn lại]*
- 🟡 Không phân định hư hỏng nhỏ (người thuê) / hư hỏng lớn (chủ nhà).
- 🟢 Phân định rõ: bóng đèn, vòi nước nhỏ… người thuê; kết cấu, thấm dột, thiết bị có sẵn hư do lỗi tự nhiên… chủ nhà, kèm thời hạn sửa.
- Câu hỏi gợi ý: "Nếu máy lạnh/bình nóng lạnh có sẵn bị hư không do em thì ai chịu chi phí, sửa trong bao lâu?"

### 3.14 `TAM_TRU_PHAP_LY`
- 🔴 Chủ nhà từ chối cung cấp giấy tờ/không hợp tác cho người thuê đăng ký tạm trú. *[Đ27 Luật Cư trú 2020: ở ngoài xã nơi thường trú từ 30 ngày trở lên PHẢI đăng ký tạm trú; không đăng ký bị phạt 500 nghìn–1 triệu (từ 15/12/2025 theo NĐ 282/2025/NĐ-CP Đ10, trước đó NĐ 144/2021 Đ9)]*
- 🟡 Hợp đồng không nhắc gì đến tạm trú (người thuê dễ quên, rủi ro bị phạt thuộc về mình).
- 🟢 Chủ nhà cam kết hỗ trợ đăng ký tạm trú (hoặc tự khai báo cho khách thuê) trong X ngày từ khi vào ở.
- Câu hỏi gợi ý: "Anh/chị có hỗ trợ em đăng ký tạm trú không? Cần giấy tờ gì từ phía anh/chị?"

### 3.15 `NOI_QUY_SU_DUNG`
- 🔴 Chủ nhà có quyền **tự ý vào phòng bất kỳ lúc nào không cần báo trước**. *[Hiến pháp 2013 Đ22: mọi người có quyền bất khả xâm phạm về chỗ ở; không ai được tự ý vào chỗ ở của người khác nếu không được đồng ý — áp dụng cả với phòng đang cho thuê]*
- 🔴 Cho phép chủ nhà khóa phòng / giữ đồ đạc của người thuê để xiết nợ.
- 🟡 Nội quy cấm đoán rộng nhưng mơ hồ ("không được làm ồn", "cấm tiếp khách" không rõ giờ), phạt tùy ý.
- 🟢 Nội quy cụ thể, hợp lý (giờ giấc, PCCC, khách qua đêm khai báo…), vào phòng phải báo trước trừ khẩn cấp.
- Câu hỏi gợi ý: "Khi cần kiểm tra phòng, anh/chị sẽ báo em trước bao lâu?"

### 3.16 `PHAT_VI_PHAM`
- 🔴 Phạt một chiều: chỉ người thuê bị phạt, chủ nhà vi phạm không sao; hoặc mức phạt phi lý so với vi phạm (vd đóng trễ 1 ngày phạt 1 tháng tiền thuê).
- 🟡 Có điều khoản phạt nhưng không ghi mức cụ thể. *[Đ418 BLDS 2015: phạt vi phạm phải là THỎA THUẬN ghi trong hợp đồng, mức do hai bên thống nhất — không ghi rõ thì không có căn cứ thu]*
- 🟢 Mức phạt cụ thể, tương xứng, áp dụng cho cả hai bên.
- Câu hỏi gợi ý: "Điều khoản phạt này áp dụng cho cả hai bên đúng không?"

## 4. Checklist "hợp đồng này THIẾU"

- Sau bước phân loại, mỗi mã **thiết yếu** (mục 2) không xuất hiện trong bất kỳ điều khoản nào → `checklist_items.present = false`.
- Đây là kết luận **tất định của code** (đếm slot trống), AI không tham gia.
- Hiển thị ưu tiên theo phạt thiếu M giảm dần: HOAN_COC (10) → HIEN_TRANG_TAI_SAN (8) → DON_PHUONG_CHAM_DUT (8) → GIA_THUE_THANH_TOAN (8) → TANG_GIA (7) → …

## 5. Điểm an toàn — công thức tất định

```
score = 100
        − Σ (mỗi loại có finding: RED → W, chỉ YELLOW → round(0.4 × W))
        − Σ (mỗi loại thiết yếu bị thiếu: M)
score = clamp(score, 0, 100)
```

Quyết định thiết kế (trả lời phỏng vấn):
1. **Mỗi loại chỉ trừ 1 lần** — lấy finding nặng nhất của loại đó. Tránh 3 câu mơ hồ về cùng tiền cọc bị trừ 3 lần (đo mức rủi ro theo *chủ đề*, không theo số câu).
2. **YELLOW = 40% RED** — mơ hồ đáng cảnh báo nhưng không thể nặng bằng điều khoản trái luật.
3. **Thiếu bị phạt riêng (M)** — hợp đồng "sạch" vì… không viết gì không được điểm cao: thiếu điều khoản hoàn cọc nguy hiểm ngang một điều khoản xấu.
4. AI **không tham gia** chấm điểm — tổng W và M cố định trong file này, code cộng trừ, kết quả tái lập được 100%.

Diễn giải điểm:
| Khoảng | Nhãn | Nhận định mẫu |
|---|---|---|
| 80–100 | 🟢 Khá an toàn | "Hợp đồng khá chặt chẽ, chỉ cần làm rõ vài điểm nhỏ." |
| 50–79 | 🟡 Cần làm rõ | "Có N điểm cần hỏi lại chủ trọ trước khi ký." |
| 0–49 | 🔴 Rủi ro cao | "Nhiều điều khoản bất lợi nghiêm trọng — cân nhắc kỹ trước khi ký." |

## 6. Nguồn đã tra cứu (xác minh 08/07/2026)

| Căn cứ | Nội dung dùng trong rubric | Nguồn |
|---|---|---|
| Luật Nhà ở 2023 (27/2023/QH15) Đ163, Đ172 | Nội dung bắt buộc của hợp đồng; các trường hợp đơn phương chấm dứt (k2, k3) + báo trước ≥30 ngày (k4) | [hethongphapluat Đ172](https://hethongphapluat.com/luat-nha-o-2023/dieu-172), [hethongphapluat Đ163](https://hethongphapluat.com/luat-nha-o-2023/dieu-163), [văn bản gốc](https://vanban.chinhphu.vn/?pageid=27160&docid=209627) |
| BLDS 2015 (91/2015/QH13) Đ328 | Đặt cọc: bên nhận cọc từ chối thực hiện phải trả cọc + khoản tương đương | [vksndtc](https://vksndtc.gov.vn/UserControls/Publishing/News/BinhLuan/pFormPrint.aspx?UrlListProcess=22D48E3E00E317DB107E3706F225B1CE22F006B7C704FC8B6894F6ABCA85660A&ItemID=11987&webP=portal), [lsvn.vn](https://lsvn.vn/dat-coc-va-doi-tuong-cua-dat-coc-theo-phap-luat-dan-su-viet-nam-1704107477-a139439.html) |
| BLDS 2015 Đ472, Đ473, Đ474, Đ477, Đ481 | Hợp đồng thuê tài sản, giá thuê, thời hạn, nghĩa vụ sửa chữa của bên cho thuê, trả tiền thuê (3 kỳ liên tiếp) | [thuvienphapluat](https://thuvienphapluat.vn/chinh-sach-phap-luat-moi/vn/ho-tro-phap-luat/tu-van-phap-luat/53460/nhung-dieu-can-biet-ve-hop-dong-thue-tai-san-theo-bo-luat-dan-su-2015), [nhanchinh.vn Đ477](https://nhanchinh.vn/nghia-vu-bao-dam-gia-tri-su-dung-cua-tai-san-thue-dieu-477) |
| BLDS 2015 Đ418 | Phạt vi phạm phải là thỏa thuận, mức do hai bên | [nhanchinh.vn](https://nhanchinh.vn/thoa-thuan-phat-vi-pham-dieu-418), [tapchitoaan](https://tapchitoaan.vn/ban-ve-muc-phat-vi-pham-hop-dong) |
| NĐ 133/2026/NĐ-CP (hiệu lực 25/5/2026) + QĐ 1279/QĐ-BCT (09/5/2025) | Thu tiền điện vượt giá bán lẻ: phạt 20–30 triệu, buộc hoàn trả; biểu giá sinh hoạt 6 bậc 1.984–3.460 đ/kWh (chưa gồm VAT); chi phí vận hành phải tách riêng | [baochinhphu](https://baochinhphu.vn/thu-tien-dien-nha-tro-vuot-gia-quy-dinh-co-the-bi-phat-den-30-trieu-dong-102260514221051825.htm), [EVN](https://www.evn.com.vn/d/vi-VN/news/Tu-ngay-255-phat-den-30-trieu-dong-voi-chu-nha-tro-thu-tien-dien-gia-cao-60-2025-507759), [luatvietnam biểu giá](https://luatvietnam.vn/linh-vuc-khac/bang-gia-dien-sinh-hoat-883-96993-article.html) |
| TT 44/2021/TT-BTC Đ3 | Khung giá nước sạch sinh hoạt 2.000–18.000 đ/m³, UBND tỉnh quyết định | [vanban.chinhphu](https://vanban.chinhphu.vn/?pageid=27160&docid=203448), [thuvienphapluat](https://thuvienphapluat.vn/van-ban/Tai-chinh-nha-nuoc/Thong-tu-44-2021-TT-BTC-khung-gia-nguyen-tac-phuong-phap-xac-dinh-gia-nuoc-sach-sinh-hoat-479354.aspx) |
| Luật Cư trú 2020 Đ27; NĐ 282/2025/NĐ-CP Đ10 (từ 15/12/2025, thay NĐ 144/2021 Đ9) | Ở từ 30 ngày phải đăng ký tạm trú; phạt 500k–1 triệu | [thuvienphapluat](https://thuvienphapluat.vn/phap-luat-nha-dat/muc-phat-hanh-vi-khong-dang-ky-tam-tru-theo-quy-dinh-moi-nhat-tu-15122025-11120.html) |
| NĐ 282/2025/NĐ-CP Đ11 (từ 15/12/2025, thay toàn bộ NĐ 144/2021) | Giữ thẻ căn cước của người thuê: chiếm đoạt/sử dụng trái phép phạt 2–4 triệu; nhận cầm cố thẻ căn cước phạt 8–10 triệu | [lsvn.vn](https://lsvn.vn/phat-tien-co-so-luu-tru-giu-can-cuoc-cua-khach-a167113.html), [thuvienphapluat](https://thuvienphapluat.vn/chinh-sach-phap-luat-moi/vn/ho-tro-phap-luat/chinh-sach-moi/98382/khach-san-nha-nghi-giu-the-can-cuoc-cua-khach-co-the-bi-phat-den-4-trieu-dong-tu-15-12-2025) |
| Hiến pháp 2013 Đ22 | Quyền bất khả xâm phạm về chỗ ở | văn bản gốc Hiến pháp 2013 |

> ⚠️ Ghi chú bảo trì: biểu giá điện thay đổi theo quyết định của Bộ Công Thương —
> khi giá bán lẻ điện điều chỉnh, cập nhật ngưỡng bậc 6 (hiện 3.460 đ/kWh chưa VAT,
> ~3.800 đ/kWh sau VAT) ở mục 3.7. Đây là lý do chặng 4 phải đưa các ngưỡng số
> (giá điện, giá nước, số ngày báo trước…) vào config thay vì hardcode.

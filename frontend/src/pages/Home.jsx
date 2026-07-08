import { useEffect, useState } from 'react'
import client from '../api/client.js'

/**
 * Trang chủ (skeleton chặng 1): giới thiệu sản phẩm + đèn báo kết nối backend.
 * Chặng 3 sẽ thay khối placeholder bằng ô upload ảnh/PDF thật;
 * chặng 7 thêm nút "Thử với hợp đồng mẫu".
 */
export default function Home() {
  // 'checking' | 'ok' | 'down'
  const [backend, setBackend] = useState('checking')

  useEffect(() => {
    client
      .get('/api/health')
      // Kiểm tra đúng service (tránh nhầm khi cổng 8080 đang bị app khác chiếm,
      // vd finance app cũng chạy backend ở 8080 trên cùng máy dev).
      .then((res) => setBackend(res.data.service === 'soat-tro-api' ? 'ok' : 'down'))
      .catch(() => setBackend('down'))
  }, [])

  return (
    <div className="home">
      <header className="hero">
        <h1>🏠 Soát Trọ</h1>
        <p className="tagline">
          Chụp hợp đồng thuê trọ — 30 giây sau biết điều khoản nào rủi ro,
          hợp đồng <strong>thiếu</strong> gì, và cần hỏi lại chủ trọ câu gì.
        </p>

        <div className="upload-placeholder">
          <p>📄 Khu vực upload hợp đồng (ảnh / PDF)</p>
          <p className="hint">— sẽ hoàn thiện ở chặng 3 —</p>
        </div>

        <p className={`backend-status ${backend}`}>
          {backend === 'checking' && 'Đang kiểm tra kết nối backend…'}
          {backend === 'ok' && '✅ Backend sẵn sàng'}
          {backend === 'down' && '⚠️ Chưa kết nối được backend (hãy chạy Spring Boot ở cổng 8080)'}
        </p>
      </header>

      <footer className="disclaimer">
        Soát Trọ là công cụ tham khảo, không phải tư vấn pháp lý.
      </footer>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import { ResultView } from './Result.jsx'

/**
 * Trang "Thử với hợp đồng mẫu" (chặng 7): tải kết quả demo dựng sẵn từ backend
 * (không gọi Gemini) rồi hiển thị bằng đúng giao diện trang kết quả.
 */
export default function Demo() {
  const [analysis, setAnalysis] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    client
      .get('/api/analyses/sample')
      .then((res) => setAnalysis(res.data))
      .catch(() => setError('Không tải được hợp đồng mẫu. Hãy thử lại.'))
  }, [])

  if (error) {
    return (
      <div className="result-page">
        <p className="load-msg">{error}</p>
        <Link className="btn-ghost" to="/">← Về trang chủ</Link>
      </div>
    )
  }
  if (!analysis) {
    return <div className="result-page"><p className="load-msg">Đang tải hợp đồng mẫu…</p></div>
  }
  return <ResultView analysis={analysis} demo />
}

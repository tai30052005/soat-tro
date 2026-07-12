import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client.js'
import { isLoggedIn } from '../lib/auth.js'
import { scoreTone } from '../lib/risk.js'

/** Lịch sử các lần soát của user đang đăng nhập (chặng 6). */
export default function History() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (!isLoggedIn()) {
      navigate('/login', { state: { from: '/history' }, replace: true })
      return
    }
    client
      .get('/api/analyses')
      .then((res) => setItems(res.data))
      .catch((err) => {
        if (err.response?.status === 401) {
          navigate('/login', { state: { from: '/history' }, replace: true })
        } else {
          setError('Không tải được lịch sử. Hãy thử lại.')
        }
      })
  }, [navigate])

  if (error) return <div className="page-narrow"><p className="load-msg">{error}</p></div>
  if (items === null) return <div className="page-narrow"><p className="load-msg">Đang tải lịch sử…</p></div>

  return (
    <div className="page-narrow">
      <h1>Lịch sử soát hợp đồng</h1>
      {items.length === 0 ? (
        <div className="empty-history">
          <p>Bạn chưa soát hợp đồng nào.</p>
          <Link className="btn-primary" to="/">Soát hợp đồng đầu tiên</Link>
        </div>
      ) : (
        <ul className="history-list">
          {items.map((it) => (
            <li key={it.id}>
              <Link to={`/analyses/${it.id}`} className={`history-item ${scoreTone(it.safetyScore)}`}>
                <span className="hi-score">
                  {it.status === 'COMPLETED' ? (it.safetyScore ?? '—') : statusIcon(it.status)}
                </span>
                <span className="hi-body">
                  <span className="hi-verdict">
                    {it.status === 'COMPLETED'
                      ? (it.verdictLabel || 'Đã soát xong')
                      : statusLabel(it.status)}
                  </span>
                  <span className="hi-date">{formatDate(it.createdAt)}</span>
                </span>
                <span className="hi-caret">→</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function statusIcon(status) {
  if (status === 'FAILED') return '⚠️'
  return '⏳'
}

function statusLabel(status) {
  if (status === 'FAILED') return 'Soát thất bại'
  return 'Đang soát…'
}

function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client.js'

const ACCEPT = 'image/jpeg,image/png,image/webp,application/pdf'
const MAX_FILES = 10

// Thông điệp xoay vòng ở màn hình chờ (~20–30s) để người dùng biết máy đang làm gì.
const PROGRESS_MSGS = [
  'Đang đọc hợp đồng…',
  'Đang bóc tách từng điều khoản…',
  'Đang đối chiếu với 16 điểm kiểm tra…',
  'Đang rà các điều khoản còn thiếu…',
  'Sắp xong rồi…',
]

/**
 * Trang chủ + upload hợp đồng (chặng 5). Không bắt đăng nhập.
 * Gửi multipart tới POST /api/analyses rồi chuyển sang trang kết quả.
 */
export default function Home() {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const [msgIdx, setMsgIdx] = useState(0)
  const [error, setError] = useState(null)
  const inputRef = useRef(null)
  const navigate = useNavigate()

  const addFiles = (list) => {
    setError(null)
    const picked = Array.from(list).slice(0, MAX_FILES)
    setFiles(picked)
  }

  const onDrop = (e) => {
    e.preventDefault()
    if (e.dataTransfer.files?.length) addFiles(e.dataTransfer.files)
  }

  const submit = async () => {
    if (files.length === 0) {
      setError('Hãy chọn ít nhất 1 ảnh hoặc file PDF của hợp đồng.')
      return
    }
    setError(null)
    setLoading(true)

    // Xoay thông điệp chờ.
    const timer = setInterval(
      () => setMsgIdx((i) => (i + 1) % PROGRESS_MSGS.length),
      3500,
    )

    try {
      const form = new FormData()
      files.forEach((f) => form.append('files', f))
      const res = await client.post('/api/analyses', form)
      navigate(`/analyses/${res.data.id}`, { state: { analysis: res.data } })
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Có lỗi khi soát hợp đồng. Hãy thử lại sau ít phút.'
      setError(msg)
    } finally {
      clearInterval(timer)
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="home loading-screen">
        <div className="spinner" aria-hidden />
        <p className="progress-msg">{PROGRESS_MSGS[msgIdx]}</p>
        <p className="progress-sub">Thường mất khoảng 20–30 giây, đừng đóng trang nhé.</p>
      </div>
    )
  }

  return (
    <div className="home">
      <header className="hero">
        <h1>🏠 Soát Trọ</h1>
        <p className="tagline">
          Chụp hợp đồng thuê trọ — 30 giây sau biết điều khoản nào rủi ro,
          hợp đồng <strong>thiếu</strong> gì, và cần hỏi lại chủ trọ câu gì.
        </p>

        <div
          className={`dropzone ${files.length ? 'has-files' : ''}`}
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => e.preventDefault()}
          onDrop={onDrop}
          role="button"
          tabIndex={0}
        >
          <input
            ref={inputRef}
            type="file"
            accept={ACCEPT}
            multiple
            hidden
            onChange={(e) => addFiles(e.target.files)}
          />
          {files.length === 0 ? (
            <>
              <p className="dz-icon">📄</p>
              <p className="dz-main">Kéo thả ảnh/PDF vào đây, hoặc bấm để chọn</p>
              <p className="dz-hint">Chụp nhiều trang cũng được · JPG/PNG/WEBP/PDF · tối đa {MAX_FILES} ảnh</p>
            </>
          ) : (
            <>
              <p className="dz-count">Đã chọn {files.length} file:</p>
              <ul className="file-list">
                {files.map((f, i) => (
                  <li key={i}>📎 {f.name}</li>
                ))}
              </ul>
              <p className="dz-hint">Bấm để chọn lại</p>
            </>
          )}
        </div>

        {error && <p className="form-error">{error}</p>}

        <button className="btn-primary big" onClick={submit} disabled={files.length === 0}>
          Soát hợp đồng
        </button>
      </header>

      <footer className="disclaimer">
        Soát Trọ là công cụ tham khảo, không phải tư vấn pháp lý.
      </footer>
    </div>
  )
}

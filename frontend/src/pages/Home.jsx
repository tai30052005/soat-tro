import { useRef, useState } from 'react'
import { motion, useReducedMotion } from 'motion/react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client.js'
import ScanLoader from '../components/ScanLoader.jsx'

const ACCEPT = 'image/jpeg,image/png,image/webp,application/pdf'
const MAX_FILES = 10

// Entrance nhẹ 1 lần khi vào trang: các khối "trồi" lên lần lượt (stagger ngắn).
const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.09, delayChildren: 0.02 } },
}
const rise = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.45, ease: [0.23, 1, 0.32, 1] } },
}

/**
 * Trang chủ + upload hợp đồng (chặng 5, làm mới theo concept "soi kỹ từng dòng").
 * Không bắt đăng nhập. Gửi multipart tới POST /api/analyses rồi chuyển sang trang kết quả.
 */
export default function Home() {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const inputRef = useRef(null)
  const navigate = useNavigate()
  const reduce = useReducedMotion()

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

    try {
      const form = new FormData()
      files.forEach((f) => form.append('files', f))
      const res = await client.post('/api/analyses', form)
      // Mang theo file để trang kết quả xem lại ảnh gốc (chỉ trong trình duyệt,
      // KHÔNG gửi/lưu ở server — giữ đúng nguyên tắc riêng tư).
      navigate(`/analyses/${res.data.id}`, { state: { analysis: res.data, files } })
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Có lỗi khi soát hợp đồng. Hãy thử lại sau ít phút.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="home loading-screen">
        <ScanLoader sub="Thường mất khoảng 20–30 giây, đừng đóng trang nhé." />
      </div>
    )
  }

  return (
    <div className="home">
      <motion.header
        className="hero"
        variants={stagger}
        initial={reduce ? false : 'hidden'}
        animate="show"
      >
        <motion.div className="hero-copy" variants={rise}>
          <h1>
            Ký hợp đồng trọ?
            <br />
            <span className="hl">Soát trước đã.</span>
          </h1>
          <p className="tagline">
            Chụp hợp đồng thuê trọ — khoảng 30 giây sau biết điều khoản nào rủi ro,
            hợp đồng <strong>thiếu</strong> gì, và cần hỏi lại chủ trọ câu gì.
          </p>
          <ul className="hero-assur">
            <li>Ảnh hợp đồng không rời khỏi máy bạn — không lưu trên máy chủ</li>
            <li>Đối chiếu với các điều khoản thường bị gài & mục còn thiếu</li>
            <li>Miễn phí, không cần đăng nhập</li>
          </ul>
        </motion.div>

        <motion.div className="hero-panel" variants={rise}>
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
                <p className="dz-main">Kéo thả ảnh/PDF vào đây, hoặc bấm để chọn</p>
                <p className="dz-hint">Nhiều trang cũng được · JPG/PNG/WEBP/PDF · tối đa {MAX_FILES} ảnh</p>
              </>
            ) : (
              <>
                <p className="dz-count">Đã chọn {files.length} file:</p>
                <ul className="file-list">
                  {files.map((f, i) => (
                    <li key={i}>{f.name}</li>
                  ))}
                </ul>
                <p className="dz-hint">Bấm để chọn lại</p>
              </>
            )}
          </div>

          {error && <p className="form-error">{error}</p>}

          <div className="cta-row">
            <button className="btn-primary big" onClick={submit} disabled={files.length === 0}>
              Soát hợp đồng
            </button>
            <Link className="btn-secondary big" to="/demo">
              Xem thử hợp đồng mẫu
            </Link>
          </div>
        </motion.div>
      </motion.header>

      <motion.section
        className="steps"
        aria-label="Cách hoạt động"
        variants={stagger}
        initial={reduce ? false : 'hidden'}
        whileInView="show"
        viewport={{ once: true, margin: '-80px' }}
      >
        <motion.div className="step" variants={rise}>
          <span className="step-n">01</span>
          <h3>Chụp hợp đồng</h3>
          <p>Ảnh điện thoại hoặc PDF, nhiều trang cũng được</p>
        </motion.div>
        <motion.div className="step" variants={rise}>
          <span className="step-n">02</span>
          <h3>AI đối chiếu điều khoản</h3>
          <p>Giá điện nước, tiền cọc, quyền đuổi, tăng giá…</p>
        </motion.div>
        <motion.div className="step" variants={rise}>
          <span className="step-n">03</span>
          <h3>Nhận điểm & câu hỏi</h3>
          <p>Điểm an toàn, mục còn thiếu, câu hỏi cầm đi hỏi chủ trọ</p>
        </motion.div>
      </motion.section>

      <footer className="footband">Soát Trọ là công cụ tham khảo, không phải tư vấn pháp lý.</footer>
    </div>
  )
}

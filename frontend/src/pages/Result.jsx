import { useEffect, useMemo, useState } from 'react'
import { motion, animate, useReducedMotion } from 'motion/react'
import { useLocation, useParams, Link } from 'react-router-dom'
import client from '../api/client.js'
import ScanLoader from '../components/ScanLoader.jsx'
import { RISK_META, scoreTone, buildBlocks, buildQuestions } from '../lib/risk.js'

// Chu vi vòng gauge (r = 50): dùng để tô cung tròn theo % điểm an toàn.
const GAUGE_CIRC = 2 * Math.PI * 50

const PENDING = ['PENDING', 'PROCESSING']

/**
 * Trang kết quả soát hợp đồng (chặng 5 + poll bất đồng bộ chặng 6).
 * 4 khối theo SPEC: ① điểm an toàn ② hợp đồng tô màu ③ checklist thiếu ④ câu hỏi hỏi chủ trọ.
 *
 * Dữ liệu: ưu tiên state từ trang upload (có clauses để tô màu). Vì pipeline chạy nền,
 * nếu trạng thái còn PROCESSING thì poll GET /api/analyses/{id} tới khi COMPLETED/FAILED.
 */
export default function Result() {
  const { id } = useParams()
  const location = useLocation()
  const initial = location.state?.analysis || null
  const uploaded = location.state?.files || null   // File gốc từ trang upload (nếu có)
  const [analysis, setAnalysis] = useState(initial)
  const [error, setError] = useState(null)

  useEffect(() => {
    // Đã có kết quả cuối (COMPLETED/FAILED) truyền sẵn -> khỏi poll.
    if (initial && !PENDING.includes(initial.status)) return

    let cancelled = false
    let timer = null
    const poll = () => {
      client
        .get(`/api/analyses/${id}`)
        .then((res) => {
          if (cancelled) return
          setAnalysis(res.data)
          if (PENDING.includes(res.data.status)) {
            timer = setTimeout(poll, 2500)   // chưa xong -> hỏi lại sau 2.5s
          }
        })
        .catch(() => {
          if (!cancelled) setError('Không tải được kết quả. Có thể lượt soát này không tồn tại.')
        })
    }
    poll()

    return () => {
      cancelled = true
      if (timer) clearTimeout(timer)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  if (error) {
    return (
      <div className="result-page">
        <p className="load-msg">{error}</p>
        <Link className="btn-ghost" to="/">← Soát hợp đồng khác</Link>
      </div>
    )
  }
  if (!analysis || PENDING.includes(analysis.status)) {
    return (
      <div className="home loading-screen">
        <ScanLoader sub="Đang đọc và đối chiếu từng điều khoản, thường mất 20–30 giây." />
      </div>
    )
  }
  if (analysis.status === 'FAILED') {
    return (
      <div className="result-page">
        <div className="failed-card">
          <h2>Chưa soát được hợp đồng này</h2>
          <p>{analysis.errorMessage || 'Ảnh không đọc được. Hãy chụp rõ nét, đủ sáng và thử lại.'}</p>
          <Link className="btn-primary" to="/">Thử lại</Link>
        </div>
      </div>
    )
  }

  return <ResultView analysis={analysis} files={uploaded} />
}

export function ResultView({ analysis, demo = false, files = null }) {
  const blocks = useMemo(() => buildBlocks(analysis), [analysis])
  const questions = useMemo(() => buildQuestions(analysis), [analysis])
  const missing = (analysis.checklist || []).filter((c) => !c.present)
  const present = (analysis.checklist || []).filter((c) => c.present)
  const tone = scoreTone(analysis.safetyScore)

  // Số liệu cho dòng badge tổng hợp dưới verdict.
  const redCount = (analysis.findings || []).filter((f) => f.riskLevel === 'RED').length
  const yellowCount = (analysis.findings || []).filter((f) => f.riskLevel === 'YELLOW').length
  const finalScore = analysis.safetyScore
  const gaugeDash = finalScore != null ? (finalScore / 100) * GAUGE_CIRC : 0

  // Khoảnh khắc trả kết quả: cung tròn "chạy" từ 0 -> điểm và số đếm lên khi mount.
  // Cùng một spring (motion) lo cả cung lẫn số để hai thứ khớp nhịp tuyệt đối.
  // Tôn trọng prefers-reduced-motion: hiện thẳng giá trị cuối, không hoạt cảnh.
  const reduce = useReducedMotion()
  const [shownScore, setShownScore] = useState(reduce || finalScore == null ? finalScore : 0)

  useEffect(() => {
    if (reduce || finalScore == null) {
      setShownScore(finalScore)
      return undefined
    }
    const controls = animate(0, finalScore, {
      duration: 0.9,
      ease: [0.23, 1, 0.32, 1], // easeOutCubic mạnh — cùng "chất" với curve của cung
      onUpdate: (v) => setShownScore(Math.round(v)),
    })
    return () => controls.stop()
  }, [reduce, finalScore])

  // Ảnh gốc để xem lại — dựng URL tạm ngay trong trình duyệt, KHÔNG tải lên server.
  // Chỉ ảnh (PDF không xem trước được ở đây). Thu hồi URL khi rời trang để không rò bộ nhớ.
  const uploads = useMemo(() => {
    if (!files || files.length === 0) return []
    return Array.from(files)
      .filter((f) => f.type && f.type.startsWith('image/'))
      .map((f) => ({ name: f.name, url: URL.createObjectURL(f) }))
  }, [files])
  useEffect(() => () => uploads.forEach((u) => URL.revokeObjectURL(u.url)), [uploads])

  return (
    <div className="result-page">
      <div className="result-top">
        <Link className="btn-ghost" to="/">← Soát hợp đồng khác</Link>
      </div>

      {demo && (
        <div className="demo-banner">
          👀 Đây là <strong>hợp đồng mẫu</strong> để bạn xem thử kết quả. Bấm “Soát hợp đồng khác” để tải hợp đồng của bạn.
        </div>
      )}

      {/* ① Điểm an toàn — vòng gauge tô cung theo % điểm */}
      <section className={`score-card ${tone}`}>
        <div className="score-gauge">
          <svg viewBox="0 0 118 118" aria-hidden="true">
            <circle className="gauge-track" cx="59" cy="59" r="50" />
            {gaugeDash > 0 && (
              <motion.circle
                className="gauge-arc"
                cx="59" cy="59" r="50"
                strokeDasharray={GAUGE_CIRC.toFixed(1)}
                initial={{ strokeDashoffset: reduce ? GAUGE_CIRC - gaugeDash : GAUGE_CIRC }}
                animate={{ strokeDashoffset: GAUGE_CIRC - gaugeDash }}
                transition={reduce ? { duration: 0 } : { type: 'spring', duration: 0.9, bounce: 0 }}
              />
            )}
          </svg>
          <div className="gauge-val">
            <span className="score-num">{shownScore ?? '—'}</span>
            <span className="score-den">/100</span>
          </div>
        </div>
        <div className="score-verdict">
          <h1>{analysis.verdictLabel || 'Kết quả soát hợp đồng'}</h1>
          {analysis.summary && <p className="score-summary">{analysis.summary}</p>}
          {(redCount > 0 || yellowCount > 0 || missing.length > 0) && (
            <div className="score-badges">
              {redCount > 0 && <span className="badge badge-red">{redCount} 🔴 nghiêm trọng</span>}
              {yellowCount > 0 && <span className="badge badge-yellow">{yellowCount} 🟡 cần hỏi lại</span>}
              {missing.length > 0 && <span className="badge badge-mute">✗ thiếu {missing.length} mục thiết yếu</span>}
            </div>
          )}
          <p className="disclaimer-inline">
            ⚠️ Soát Trọ là công cụ tham khảo, không phải tư vấn pháp lý.
          </p>
        </div>
      </section>

      <div className="result-grid">
        {/* ② Hợp đồng tô màu */}
        <section className="panel contract-panel">
          <h2>Điều khoản trong hợp đồng</h2>
          {uploads.length > 0 && (
            <details className="uploaded-images">
              <summary>📷 Ảnh hợp đồng bạn đã tải ({uploads.length}) — bấm để xem</summary>
              <div className="img-strip">
                {uploads.map((u, i) => (
                  <a key={i} href={u.url} target="_blank" rel="noreferrer" title="Bấm để xem ảnh to">
                    <img src={u.url} alt={u.name} loading="lazy" />
                  </a>
                ))}
              </div>
            </details>
          )}
          <p className="panel-hint">Bấm vào điều khoản có màu để xem giải thích và căn cứ.</p>
          <div className="clause-list">
            {blocks.length === 0 && <p className="empty">Không có nội dung điều khoản.</p>}
            {blocks.map((b, i) => (
              <ClauseBlock key={i} block={b} index={i} />
            ))}
          </div>
        </section>

        <div className="side-col">
          {/* ③ Checklist thiếu */}
          <section className="panel">
            <h2>Hợp đồng này còn thiếu</h2>
            {missing.length === 0 ? (
              <p className="all-good">✅ Có đủ các điều khoản thiết yếu.</p>
            ) : (
              <ul className="check-list missing">
                {missing.map((c) => (
                  <li key={c.clauseType}><span className="mark">✗</span> {c.label}</li>
                ))}
              </ul>
            )}
            {present.length > 0 && (
              <details className="present-fold">
                <summary>Đã có {present.length} điều khoản thiết yếu</summary>
                <ul className="check-list present">
                  {present.map((c) => (
                    <li key={c.clauseType}><span className="mark">✓</span> {c.label}</li>
                  ))}
                </ul>
              </details>
            )}
          </section>

          {/* ④ Câu hỏi hỏi chủ trọ */}
          <QuestionsPanel questions={questions} />
        </div>
      </div>
    </div>
  )
}

function ClauseBlock({ block, index = 0 }) {
  const [open, setOpen] = useState(false)
  const reduce = useReducedMotion()
  const meta = RISK_META[block.risk] || RISK_META.NONE
  const hasDetail = block.findings.length > 0
  // Reveal "đèn quét tới đâu hiện tới đó": các block hiện lần lượt (stagger cap ~12 block
  // để không lê thê), spring nhẹ. Reduced-motion -> hiện thẳng.
  return (
    <motion.div
      className={`clause ${meta.className} ${open ? 'open' : ''}`}
      initial={reduce ? false : { opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: Math.min(index, 12) * 0.04, duration: 0.35, ease: [0.23, 1, 0.32, 1] }}
    >
      <button
        className="clause-head"
        onClick={() => hasDetail && setOpen((o) => !o)}
        aria-expanded={open}
        disabled={!hasDetail}
      >
        <span className="clause-dot">{meta.dot}</span>
        <span className="clause-text">{block.text}</span>
        {hasDetail && <span className="clause-caret">{open ? '▾' : '▸'}</span>}
      </button>
      {open && (
        <div className="clause-detail">
          {block.findings.map((f, i) => (
            <div key={i} className="finding">
              <p className="finding-exp">{f.explanation}</p>
              {f.suggestion && <p className="finding-sug">💡 {f.suggestion}</p>}
              {f.lawRef && <p className="finding-law">📖 Căn cứ: {f.lawRef}</p>}
            </div>
          ))}
        </div>
      )}
    </motion.div>
  )
}

function QuestionsPanel({ questions }) {
  const [copied, setCopied] = useState(false)
  if (questions.length === 0) return null

  const asText = questions.map((q, i) => `${i + 1}. ${q}`).join('\n')
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(asText)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      /* clipboard bị chặn (http) — bỏ qua, người dùng vẫn đọc/in được */
    }
  }

  return (
    <section className="panel questions-panel">
      <h2>Câu hỏi cầm đi hỏi chủ trọ</h2>
      <ol className="questions">
        {questions.map((q, i) => (
          <li key={i}>{q}</li>
        ))}
      </ol>
      <div className="q-actions">
        <button className="btn-ghost" onClick={copy}>
          {copied ? '✓ Đã copy' : '📋 Copy danh sách'}
        </button>
        <button className="btn-ghost" onClick={() => window.print()}>🖨️ In</button>
      </div>
    </section>
  )
}

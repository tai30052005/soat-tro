import { useEffect, useMemo, useState } from 'react'
import { useLocation, useParams, Link } from 'react-router-dom'
import client from '../api/client.js'
import { RISK_META, scoreTone, buildBlocks, buildQuestions } from '../lib/risk.js'

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
        <div className="spinner" aria-hidden />
        <p className="progress-msg">Đang soát hợp đồng…</p>
        <p className="progress-sub">Đang đọc và đối chiếu từng điều khoản, thường mất 20–30 giây.</p>
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

  return <ResultView analysis={analysis} />
}

export function ResultView({ analysis, demo = false }) {
  const blocks = useMemo(() => buildBlocks(analysis), [analysis])
  const questions = useMemo(() => buildQuestions(analysis), [analysis])
  const missing = (analysis.checklist || []).filter((c) => !c.present)
  const present = (analysis.checklist || []).filter((c) => c.present)
  const tone = scoreTone(analysis.safetyScore)

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

      {/* ① Điểm an toàn */}
      <section className={`score-card ${tone}`}>
        <div className="score-ring">
          <span className="score-num">{analysis.safetyScore ?? '—'}</span>
          <span className="score-den">/100</span>
        </div>
        <div className="score-verdict">
          <h1>{analysis.verdictLabel || 'Kết quả soát hợp đồng'}</h1>
          {analysis.summary && <p className="score-summary">{analysis.summary}</p>}
          <p className="disclaimer-inline">
            ⚠️ Soát Trọ là công cụ tham khảo, không phải tư vấn pháp lý.
          </p>
        </div>
      </section>

      <div className="result-grid">
        {/* ② Hợp đồng tô màu */}
        <section className="panel contract-panel">
          <h2>Điều khoản trong hợp đồng</h2>
          <p className="panel-hint">Bấm vào điều khoản có màu để xem giải thích và căn cứ.</p>
          <div className="clause-list">
            {blocks.length === 0 && <p className="empty">Không có nội dung điều khoản.</p>}
            {blocks.map((b, i) => (
              <ClauseBlock key={i} block={b} />
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

function ClauseBlock({ block }) {
  const [open, setOpen] = useState(false)
  const meta = RISK_META[block.risk] || RISK_META.NONE
  const hasDetail = block.findings.length > 0
  return (
    <div className={`clause ${meta.className} ${open ? 'open' : ''}`}>
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
    </div>
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

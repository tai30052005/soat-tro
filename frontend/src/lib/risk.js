// Tiện ích hiển thị rủi ro cho trang kết quả (chặng 5).
// Toàn bộ logic ở đây chỉ để TRÌNH BÀY — không quyết định điểm/kết luận
// (điểm & checklist đã do backend tính tất định).

/** Thứ hạng mức rủi ro để so "nặng hơn" (RED > YELLOW > GREEN). */
const RANK = { RED: 3, YELLOW: 2, GREEN: 1 }

/** Thông tin hiển thị cho từng mức rủi ro. */
export const RISK_META = {
  RED: { dot: '🔴', text: 'Rủi ro cao', className: 'risk-red' },
  YELLOW: { dot: '🟡', text: 'Cần làm rõ', className: 'risk-yellow' },
  GREEN: { dot: '🟢', text: 'Ổn', className: 'risk-green' },
  NONE: { dot: '⚪', text: 'Chưa phát hiện rủi ro', className: 'risk-none' },
}

/** Màu chủ đạo của trang theo điểm an toàn (khớp ngưỡng backend: ≥80 / ≥50). */
export function scoreTone(score) {
  if (score == null) return 'risk-none'
  if (score >= 80) return 'risk-green'
  if (score >= 50) return 'risk-yellow'
  return 'risk-red'
}

/** Chọn finding "nặng nhất" trong danh sách (để tô màu 1 điều khoản). */
export function worst(findings) {
  return findings.reduce((acc, f) => {
    if (!acc || (RANK[f.riskLevel] || 0) > (RANK[acc.riskLevel] || 0)) return f
    return acc
  }, null)
}

/**
 * Ghép từng điều khoản (block văn bản) với các finding tương ứng.
 *  - Sau POST: có `clauses` (kèm clauseType) -> khớp theo clauseType.
 *  - Khi xem lại (GET): `clauses` rỗng -> tách contractText theo dòng trống,
 *    khớp finding bằng cách tìm quote nằm trong block.
 */
export function buildBlocks(analysis) {
  const findings = analysis.findings || []
  let blocks

  if (analysis.clauses && analysis.clauses.length > 0) {
    blocks = analysis.clauses.map((c) => ({
      text: c.text,
      clauseType: c.clauseType,
      label: c.label,
    }))
  } else if (analysis.contractText) {
    blocks = analysis.contractText
      .split(/\n{2,}/)
      .map((t) => t.trim())
      .filter(Boolean)
      .map((text) => ({ text, clauseType: null, label: null }))
  } else {
    blocks = []
  }

  return blocks.map((b) => {
    const matched = findings.filter(
      (f) =>
        (b.clauseType && f.clauseType === b.clauseType) ||
        (f.quote && b.text.includes(f.quote)),
    )
    const w = worst(matched)
    return { ...b, findings: matched, risk: w ? w.riskLevel : 'NONE' }
  })
}

/**
 * "Câu hỏi cầm đi hỏi chủ trọ" — suy ra tất định từ findings + checklist thiếu,
 * KHÔNG nhờ AI sinh. Ưu tiên: RED trước, rồi YELLOW, rồi điều khoản còn thiếu.
 */
export function buildQuestions(analysis) {
  const questions = []
  const seen = new Set()
  const push = (q) => {
    const key = q.trim().toLowerCase()
    if (q && !seen.has(key)) {
      seen.add(key)
      questions.push(q.trim())
    }
  }

  const findings = analysis.findings || []
  const bySeverity = [...findings].sort(
    (a, b) => (RANK[b.riskLevel] || 0) - (RANK[a.riskLevel] || 0),
  )
  for (const f of bySeverity) {
    if (f.suggestion) push(f.suggestion)
  }

  for (const item of analysis.checklist || []) {
    if (!item.present) {
      push(`Hợp đồng chưa có điều khoản "${item.label}" — đề nghị chủ trọ bổ sung và ghi rõ.`)
    }
  }

  return questions
}

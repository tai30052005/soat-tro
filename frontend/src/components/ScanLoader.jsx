import { useEffect, useState } from 'react'

// Thông điệp xoay vòng theo ngôn ngữ "soi" của concept.
const DEFAULT_MSGS = [
  '🔍 Đang đọc hợp đồng…',
  '💰 Đang soi điều khoản tiền cọc…',
  '⚡ Đang soi giá điện, giá nước…',
  '📋 Đang rà các điều khoản còn thiếu…',
  'Sắp xong rồi…',
]

/**
 * Màn hình chờ "máy quét hợp đồng" (concept "soi kỹ từng dòng"):
 * vệt đèn teal quét dọc tờ giấy, quét tới dòng rủi ro thì dòng đó lóe đỏ/vàng.
 * Dùng chung cho trang chủ (đang upload) và trang kết quả (đang poll).
 */
export default function ScanLoader({ messages = DEFAULT_MSGS, sub }) {
  const [idx, setIdx] = useState(0)

  useEffect(() => {
    if (messages.length <= 1) return undefined
    const timer = setInterval(() => setIdx((i) => (i + 1) % messages.length), 3400)
    return () => clearInterval(timer)
  }, [messages])

  return (
    <div className="scan-loader" role="status">
      <div className="scanpaper" aria-hidden="true">
        <div className="sl head" />
        <div className="sl" />
        <div className="sl w85" />
        <div className="sl w70 flag-red" />
        <div className="sl" />
        <div className="sl w55" />
        <div className="sl w85 flag-yellow" />
        <div className="sl w70" />
        <div className="sl" />
        <div className="sl w85 flag-red2" />
        <div className="sl w55" />
        <div className="beam" />
      </div>
      <p className="progress-msg">{messages[idx]}</p>
      {sub && <p className="progress-sub">{sub}</p>}
    </div>
  )
}

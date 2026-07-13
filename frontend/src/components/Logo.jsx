/**
 * Logo "Soát Trọ" (concept "soi kỹ từng dòng"): ngôi nhà chứa các dòng hợp đồng,
 * dòng đang được "rọi đèn" sáng màu teal. Vẽ SVG thuần — không cần file ảnh.
 */
export default function Logo({ size = 26 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
      <path
        d="M8 22 L24 8 L40 22"
        fill="none" stroke="#1f2937" strokeWidth="3.8"
        strokeLinecap="round" strokeLinejoin="round"
      />
      <path
        d="M12 20 V40 H36 V20"
        fill="none" stroke="#1f2937" strokeWidth="3.8"
        strokeLinecap="round" strokeLinejoin="round"
      />
      <line x1="17" y1="26.5" x2="31" y2="26.5" stroke="#9ca3af" strokeWidth="2.6" strokeLinecap="round" />
      <line x1="17" y1="31.5" x2="31" y2="31.5" stroke="#0d9488" strokeWidth="3.6" strokeLinecap="round" />
      <line x1="17" y1="36" x2="26" y2="36" stroke="#9ca3af" strokeWidth="2.6" strokeLinecap="round" />
    </svg>
  )
}

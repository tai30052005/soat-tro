import { Route, Routes } from 'react-router-dom'
import Home from './pages/Home.jsx'
import Result from './pages/Result.jsx'

/**
 * Khung định tuyến của app.
 *  - /               : trang chủ + upload hợp đồng
 *  - /analyses/:id   : trang kết quả soát
 * Chặng 6 thêm: /login, /register, /history.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/analyses/:id" element={<Result />} />
    </Routes>
  )
}

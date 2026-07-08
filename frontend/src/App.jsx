import { Route, Routes } from 'react-router-dom'
import Home from './pages/Home.jsx'

/**
 * Khung định tuyến của app. Chặng 1 mới có trang chủ placeholder;
 * các chặng sau thêm: /analyses/:id (kết quả), /login, /register, /history.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
    </Routes>
  )
}

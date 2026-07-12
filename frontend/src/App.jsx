import { Route, Routes } from 'react-router-dom'
import Nav from './components/Nav.jsx'
import Home from './pages/Home.jsx'
import Result from './pages/Result.jsx'
import Demo from './pages/Demo.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import History from './pages/History.jsx'

/**
 * Khung định tuyến của app (chặng 6).
 *  - /               : trang chủ + upload hợp đồng
 *  - /analyses/:id   : trang kết quả soát (poll khi đang xử lý)
 *  - /login /register: xác thực (chỉ để lưu lịch sử)
 *  - /history        : lịch sử soát của user đăng nhập
 */
export default function App() {
  return (
    <>
      <Nav />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/analyses/:id" element={<Result />} />
        <Route path="/demo" element={<Demo />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/history" element={<History />} />
      </Routes>
    </>
  )
}

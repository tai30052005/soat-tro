import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isLoggedIn, getEmail, clearAuth } from '../lib/auth.js'

/**
 * Thanh điều hướng trên cùng (chặng 6): logo về trang chủ + trạng thái đăng nhập.
 * Lắng nghe sự kiện 'auth-change' để cập nhật khi login/logout.
 */
export default function Nav() {
  const [logged, setLogged] = useState(isLoggedIn())
  const navigate = useNavigate()

  useEffect(() => {
    const sync = () => setLogged(isLoggedIn())
    window.addEventListener('auth-change', sync)
    window.addEventListener('storage', sync)
    return () => {
      window.removeEventListener('auth-change', sync)
      window.removeEventListener('storage', sync)
    }
  }, [])

  const logout = () => {
    clearAuth()
    navigate('/')
  }

  return (
    <nav className="nav">
      <Link to="/" className="nav-brand">🏠 Soát Trọ</Link>
      <div className="nav-links">
        {logged ? (
          <>
            <Link to="/history" className="nav-link">Lịch sử</Link>
            <span className="nav-email" title={getEmail()}>{getEmail()}</span>
            <button className="nav-link as-btn" onClick={logout}>Đăng xuất</button>
          </>
        ) : (
          <>
            <Link to="/login" className="nav-link">Đăng nhập</Link>
            <Link to="/register" className="nav-link primary">Đăng ký</Link>
          </>
        )}
      </div>
    </nav>
  )
}

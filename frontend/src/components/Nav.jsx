import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isLoggedIn, getEmail, clearAuth } from '../lib/auth.js'
import Logo from './Logo.jsx'

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
      <Link to="/" className="nav-brand">
        <Logo size={26} />
        <span>Soát <span className="brand-tro">Trọ</span></span>
      </Link>
      <div className="nav-links">
        {logged ? (
          <>
            <Link to="/history" className="nav-link">Lịch sử</Link>
            <span className="nav-email" title={getEmail()}>{getEmail()}</span>
            <button className="nav-link as-btn" onClick={logout}>Đăng xuất</button>
          </>
        ) : (
          <>
            <span className="nav-hint">Đăng nhập để lưu lịch sử soát (không bắt buộc)</span>
            <Link to="/login" className="nav-link"
                  title="Không bắt buộc — chỉ để lưu và xem lại lịch sử các lần soát">Đăng nhập</Link>
            <Link to="/register" className="nav-link primary"
                  title="Không bắt buộc — chỉ để lưu và xem lại lịch sử các lần soát">Đăng ký</Link>
          </>
        )}
      </div>
    </nav>
  )
}

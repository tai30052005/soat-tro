import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import client from '../api/client.js'
import { setAuth } from '../lib/auth.js'

/** Đăng nhập (chặng 6). Đăng nhập chỉ để xem lịch sử — soát hợp đồng vẫn ẩn danh. */
export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from || '/history'

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      const res = await client.post('/api/auth/login', { email, password })
      setAuth(res.data)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng nhập thất bại. Hãy thử lại.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <h1>Đăng nhập</h1>
        <p className="auth-sub">Để lưu và xem lại lịch sử các lần soát hợp đồng.</p>

        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 autoComplete="email" required />
        </label>
        <label>Mật khẩu
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete="current-password" required />
        </label>

        {error && <p className="form-error">{error}</p>}

        <button className="btn-primary big" type="submit" disabled={busy}>
          {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
        <p className="auth-alt">
          Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
        </p>
      </form>
    </div>
  )
}

import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client.js'
import { setAuth } from '../lib/auth.js'

/** Đăng ký tài khoản (chặng 6). Mật khẩu tối thiểu 8 ký tự (khớp validation backend). */
export default function Register() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    if (password.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự.')
      return
    }
    setBusy(true)
    try {
      const res = await client.post('/api/auth/register', { email, password })
      setAuth(res.data)
      navigate('/history', { replace: true })
    } catch (err) {
      const data = err.response?.data
      setError(
        data?.fieldErrors ? Object.values(data.fieldErrors)[0]
          : data?.message || 'Đăng ký thất bại. Hãy thử lại.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <h1>Đăng ký</h1>
        <p className="auth-sub">
          Tạo tài khoản để lưu lịch sử các lần soát.
          <br />Không bắt buộc — bạn vẫn soát hợp đồng bình thường khi chưa đăng nhập.
        </p>

        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 autoComplete="email" required />
        </label>
        <label>Mật khẩu
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete="new-password" required minLength={8} />
        </label>

        {error && <p className="form-error">{error}</p>}

        <button className="btn-primary big" type="submit" disabled={busy}>
          {busy ? 'Đang tạo tài khoản…' : 'Đăng ký'}
        </button>
        <p className="auth-alt">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </form>
    </div>
  )
}

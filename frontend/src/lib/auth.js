// Quản lý phiên đăng nhập phía client (chặng 6).
// Token JWT lưu ở localStorage; client.js tự gắn vào header mỗi request.

const TOKEN_KEY = 'token'
const EMAIL_KEY = 'email'

export function setAuth({ token, email }) {
  localStorage.setItem(TOKEN_KEY, token)
  if (email) localStorage.setItem(EMAIL_KEY, email)
  // Báo cho các component (Nav) cập nhật trạng thái đăng nhập.
  window.dispatchEvent(new Event('auth-change'))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EMAIL_KEY)
  window.dispatchEvent(new Event('auth-change'))
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getEmail() {
  return localStorage.getItem(EMAIL_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

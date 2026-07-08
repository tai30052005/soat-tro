import axios from 'axios'

// VITE_API_URL:
//  - Dev & Docker: để trống -> gọi "/api/..." (đường dẫn tương đối, đã có proxy).
//  - Deploy Vercel: đặt VITE_API_URL = URL backend trên Render.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
})

// Tự gắn token JWT (nếu đã đăng nhập) vào mọi request.
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default client

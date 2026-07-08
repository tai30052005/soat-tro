import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Dev: gọi "/api/..." được proxy sang backend Spring Boot (cổng 8080)
    // -> frontend và backend cùng origin, không vướng CORS khi phát triển.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})

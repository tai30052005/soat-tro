# ============================================================
#  Multi-stage build cho frontend React:
#    Stage 1 (node)  : cài deps + build ra thư mục dist/ (HTML/CSS/JS tĩnh)
#    Stage 2 (nginx) : phục vụ file tĩnh + proxy /api sang backend
# ============================================================

# ---------- STAGE 1: BUILD ----------
FROM node:22-alpine AS build
WORKDIR /app

# Copy file khai báo dependency trước để tận dụng cache
COPY package*.json ./
RUN npm ci

# Copy mã nguồn và build.
# VITE_API_URL rỗng -> frontend gọi "/api/..." (đường dẫn tương đối) để Nginx proxy.
# (Bản deploy Vercel KHÔNG dùng Dockerfile này; Vercel tự đặt VITE_API_URL = URL backend.)
COPY . .
ENV VITE_API_URL=""
RUN npm run build

# ---------- STAGE 2: SERVE ----------
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

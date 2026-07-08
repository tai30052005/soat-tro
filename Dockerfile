# ============================================================
#  Multi-stage build: tách "build" và "run" thành 2 giai đoạn
#  -> image cuối nhỏ gọn (chỉ chứa JRE + file .jar, không kèm Maven/source)
# ============================================================

# ---------- STAGE 1: BUILD ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml trước và tải dependency riêng một bước.
# Docker cache theo từng lệnh -> pom.xml không đổi thì lần build sau
# dùng lại lớp dependency đã tải (nhanh hơn nhiều).
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Giờ mới copy mã nguồn và build ra file .jar (bỏ qua test để build nhanh)
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- STAGE 2: RUNTIME ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Bảo mật: tạo user thường, không chạy app bằng root
RUN useradd --system --no-create-home spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

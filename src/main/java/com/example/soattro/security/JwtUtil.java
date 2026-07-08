package com.example.soattro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Tiện ích tạo và xác thực JWT (JSON Web Token).
 *
 * JWT là chuỗi 3 phần ngăn bởi dấu chấm: header.payload.signature
 *   - payload chứa "claims", ở đây 'subject' = email user.
 *   - signature ký bằng khóa bí mật -> server phát hiện token giả mạo/sửa đổi.
 */
@Component
public class JwtUtil {

    private final SecretKey key;        // khóa bí mật để ký & xác thực
    private final long expirationMs;    // thời hạn token (mili-giây)

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Tạo khóa HMAC-SHA từ chuỗi secret (yêu cầu >= 32 ký tự).
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Tạo token mới cho một user (subject = email). */
    public String generateToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Lấy email (subject) từ token đã được xác thực. */
    public String extractUsername(String token) {
        return parse(token).getPayload().getSubject();
    }

    /** Kiểm tra token hợp lệ (đúng chữ ký + chưa hết hạn). */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Giải mã + xác thực chữ ký. Ném exception nếu token không hợp lệ. */
    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }
}

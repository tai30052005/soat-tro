package com.example.soattro;

import com.example.soattro.security.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT là lớp bảo mật của auth — test tạo/đọc/chống giả mạo/hết hạn.
 * Dùng khóa cố định trong test (đủ 32 byte cho HS256), không phụ thuộc Spring.
 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long-0";
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtUtil jwt = new JwtUtil(SECRET, ONE_HOUR);

    @Test
    void roundTripsSubject() {
        String token = jwt.generateToken("minhtai@example.com");
        assertEquals("minhtai@example.com", jwt.extractUsername(token));
    }

    @Test
    void acceptsOwnToken() {
        assertTrue(jwt.isValid(jwt.generateToken("a@b.com")));
    }

    @Test
    void rejectsTamperedToken() {
        // Đổi 1 ký tự trong payload -> chữ ký không còn khớp -> phải bị loại.
        String token = jwt.generateToken("a@b.com");
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "B" : "A");
        assertFalse(jwt.isValid(tampered));
    }

    @Test
    void rejectsGarbageAndEmpty() {
        assertFalse(jwt.isValid("khong-phai-jwt"));
        assertFalse(jwt.isValid(""));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        // Token ký bằng khóa khác (kẻ tấn công tự phát) -> server phải từ chối.
        JwtUtil attacker = new JwtUtil("another-secret-key-also-32-bytes-long-xxxxx", ONE_HOUR);
        String forged = attacker.generateToken("admin@example.com");
        assertFalse(jwt.isValid(forged));
    }

    @Test
    void rejectsExpiredToken() {
        // Thời hạn âm -> exp nằm ở quá khứ ngay khi tạo -> phải hết hạn.
        JwtUtil shortLived = new JwtUtil(SECRET, -1_000L);
        assertFalse(shortLived.isValid(shortLived.generateToken("a@b.com")));
    }
}

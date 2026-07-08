package com.example.soattro.dto.response;

/** Phản hồi sau đăng ký/đăng nhập thành công: token + kiểu token + email. */
public record AuthResponse(String token, String tokenType, String email) {
}

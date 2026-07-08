package com.example.soattro.exception;

/** Ném khi đăng ký với email đã tồn tại -> GlobalExceptionHandler trả 409. */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email đã được sử dụng: " + email);
    }
}

package com.example.soattro.exception;

/**
 * Ném khi endpoint yêu cầu đăng nhập nhưng request ẩn danh
 * (vd xem lịch sử soát). GlobalExceptionHandler map thành 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

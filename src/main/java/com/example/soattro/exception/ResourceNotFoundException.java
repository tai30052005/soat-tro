package com.example.soattro.exception;

/** Ném khi không tìm thấy tài nguyên (hoặc thuộc user khác) -> trả 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

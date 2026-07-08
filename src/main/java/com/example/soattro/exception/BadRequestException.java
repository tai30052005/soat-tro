package com.example.soattro.exception;

/** Ném khi request sai logic nghiệp vụ (vd file không phải ảnh/PDF) -> trả 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

package com.slate.common;

import org.springframework.http.HttpStatus;

public class SlateException extends RuntimeException {

    private final HttpStatus status;

    public SlateException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public SlateException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}

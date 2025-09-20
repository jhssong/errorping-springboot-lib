package com.jhssong.errorping.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseDomainException extends RuntimeException {
    private final HttpStatus status;

    protected BaseDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}


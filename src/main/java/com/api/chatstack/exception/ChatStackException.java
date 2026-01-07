package com.api.chatstack.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ChatStackException extends RuntimeException {
    private final String reason;
    private final String code;
    private final String description;
    private final HttpStatus httpStatus;

    public ChatStackException(String reason, String code, String description, HttpStatus httpStatus) {
        super(description);
        this.reason = reason;
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }

    public ChatStackException(String reason, String description, HttpStatus httpStatus) {
        this(reason, null, description, httpStatus);
    }
}
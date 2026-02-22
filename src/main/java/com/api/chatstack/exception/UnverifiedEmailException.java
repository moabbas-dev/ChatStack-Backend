package com.api.chatstack.exception;

public class UnverifiedEmailException extends RuntimeException {
    public UnverifiedEmailException(String message) {
        super(message);
    }
}

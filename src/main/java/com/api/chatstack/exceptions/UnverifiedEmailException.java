package com.api.chatstack.exceptions;

public class UnverifiedEmailException extends RuntimeException {
    public UnverifiedEmailException(String message) {
        super(message);
    }
}

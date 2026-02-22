package com.api.chatstack.exception;

public class InvalidVerificationLinkException extends RuntimeException {
    public InvalidVerificationLinkException(String message) {
        super(message);
    }
}

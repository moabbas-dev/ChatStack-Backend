package com.api.chatstack.exceptions;

public class InvalidVerificationLinkException extends RuntimeException {
    public InvalidVerificationLinkException(String message) {
        super(message);
    }
}

package com.api.chatstack.exception;

public class NoTokenProvidedException extends RuntimeException {
    public NoTokenProvidedException(String message) {
        super(message);
    }
}

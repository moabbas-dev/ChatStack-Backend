package com.api.chatstack.exceptions;

public class NoTokenProvidedException extends RuntimeException {
    public NoTokenProvidedException(String message) {
        super(message);
    }
}

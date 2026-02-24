package com.api.chatstack.exceptions;

public class DisplayNameAlreadyTakenException extends RuntimeException {
    public DisplayNameAlreadyTakenException(String message) {
        super(message);
    }
}

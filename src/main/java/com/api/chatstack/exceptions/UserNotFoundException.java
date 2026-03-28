package com.api.chatstack.exceptions;

public class UserNotFoundException extends RuntimeException {
    public static final String ERROR_MSG = "User not found";

    public UserNotFoundException(String message) {
        super(message);
    }
}

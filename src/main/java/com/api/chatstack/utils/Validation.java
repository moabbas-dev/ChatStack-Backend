package com.api.chatstack.utils;

import com.api.chatstack.exception.ChatStackException;
import com.chatstack.dto.Error;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

public class Validation {
    public static boolean isPasswordValid(String password) {
        Error error = new Error();
        boolean result = true;

        if (password == null) {
            error.setCode("PASSWORD_REQUIRED");
            error.setReason("Invalid Length");
            error.setDescription("Password is required");
            result = false;
        } else if (password.length() < 12) {
            error.setCode("PASSWORD_TOO_SHORT");
            error.setReason("Invalid Length");
            error.setDescription("Password must be at least 12 characters");
            result = false;
        } else if (password.contains(" ")) {
            error.setCode("PASSWORD_INVALID");
            error.setReason("Invalid Password");
            error.setDescription("Password contains invalid characters");
            result = false;
        } else if (!password.matches(".*[!\"#$%&'()*+,\\-./:;<=>?@\\[\\]^_`{|}~].*")) {
            error.setCode("PASSWORD_INVALID");
            error.setReason("Invalid Password");
            error.setDescription("Password must contain at least one special character");
            result = false;
        } else if (!password.matches(".*[a-z].*")) {
            error.setCode("PASSWORD_INVALID");
            error.setReason("Invalid Password");
            error.setDescription("Password must contain at least one lowercase character");
            result = false;
        } else if (!password.matches(".*[A-Z].*")) {
            error.setCode("PASSWORD_INVALID");
            error.setReason("Invalid Password");
            error.setDescription("Password must contain at least one uppercase character");
            result = false;
        } else if (!password.matches(".*[0-9].*")) {
            error.setCode("PASSWORD_INVALID");
            error.setReason("Invalid Password");
            error.setDescription("Password must contain at least one digit");
            result = false;
        }

        if (!result) {
            throw new ChatStackException(
                    error.getReason(),
                    error.getCode(),
                    error.getDescription(),
                    HttpStatus.BAD_REQUEST
            );
        }

        return result;
    }

    public static boolean isEmailValid(String email) {
        if (email == null || email.isBlank()) {
            throw new ChatStackException(
                    "email is required",
                    "EMAIL_REQUIRED",
                    "Email is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(emailRegex);

        return pattern.matcher(email).matches();
    }

    public static boolean isUsernameValid(String username) {
        if (username == null || username.trim().isBlank()) {
            throw new ChatStackException(
                    "Invalid Username",
                    "USERNAME_REQUIRED",
                    "Username is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        username =  username.trim();
        if (username.length() < 3 || username.length() > 30) {
            throw new ChatStackException(
                    "Invalid Username",
                    "USERNAME_INVALID_LENGTH",
                    "Username must be between 3 and 30 characters",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            throw new ChatStackException(
                    "Invalid Username",
                    "USERNAME_INVALID",
                    "Username can contain only letters, numbers, dot, underscore, and hyphen",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (username.startsWith(".") || username.endsWith(".")
                || username.startsWith("_") || username.endsWith("_")
                || username.startsWith("-") || username.endsWith("-")) {
            throw new ChatStackException(
                    "Invalid Username",
                    "USERNAME_INVALID",
                    "Username cannot start or end with special characters",
                    HttpStatus.BAD_REQUEST
            );
        }
        return true;
    }

    public static String fullnameValidation(String fullname) {
        if (fullname == null || fullname.isBlank()) {
            throw new ChatStackException(
                    "Invalid Fullname",
                    "FULLNAME_INVALID",
                    "Fullname is not provided",
                    HttpStatus.BAD_REQUEST
            );
        }

        fullname = fullname.trim();
        String[] parts = fullname.split(" ");

        if (fullname.chars().filter(c -> c == ' ').count() != 1 || parts.length != 2) {
            throw new ChatStackException(
                    "Invalid Fullname",
                    "FULLNAME_INVALID",
                    "Fullname format: firstname lastname",
                    HttpStatus.BAD_REQUEST
            );
        }

        String firstName = parts[0];
        String lastName = parts[1];

        if (!firstName.matches("[a-zA-Z]+") || !lastName.matches("[a-zA-Z]+")) {
            throw new ChatStackException(
                    "Invalid Fullname",
                    "FULLNAME_INVALID",
                    "Only uppercase or lowercase are allowed for fullname",
                    HttpStatus.BAD_REQUEST
            );
        }

        firstName = firstName.substring(0,1).toUpperCase() + firstName.substring(1).toLowerCase();
        lastName = lastName.substring(0,1).toUpperCase() + lastName.substring(1).toLowerCase();

        return firstName + " " + lastName;
    }

}

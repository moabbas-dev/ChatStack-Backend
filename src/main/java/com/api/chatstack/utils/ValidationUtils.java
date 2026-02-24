package com.api.chatstack.utils;

import com.api.chatstack.exceptions.ChatStackException;
import io.micrometer.common.util.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

/**
 * Utility class for validating user input fields.
 * All methods throw {@link ChatStackException} with appropriate error codes on validation failure.
 */
public final class ValidationUtils {

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 30;

    private static final int FULLNAME_MIN_PARTS = 2;
    private static final int FULLNAME_MAX_LENGTH = 100;

    private static final Pattern USERNAME_CHARS_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern CONSECUTIVE_SPECIALS_PATTERN = Pattern.compile("[._-]{2,}");
    private static final Pattern NAME_PART_PATTERN = Pattern.compile("[a-zA-Z'-]+");

    private ValidationUtils() {}

    /**
     * Validates a username (display name).
     *
     * @param username the username to validate
     * @throws ChatStackException if the username does not meet requirements
     */
    public static void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throwValidation("USERNAME_REQUIRED", "Invalid Username", "Username is required");
        }

        String trimmed = username.trim();

        if (trimmed.length() < USERNAME_MIN_LENGTH || trimmed.length() > USERNAME_MAX_LENGTH) {
            throwValidation("USERNAME_INVALID_LENGTH", "Invalid Username",
                    "Username must be between " + USERNAME_MIN_LENGTH + " and " + USERNAME_MAX_LENGTH + " characters");
        }
        if (!USERNAME_CHARS_PATTERN.matcher(trimmed).matches()) {
            throwValidation("USERNAME_INVALID", "Invalid Username",
                    "Username can contain only letters, numbers, dot, underscore, and hyphen");
        }
        if (startsOrEndsWithSpecial(trimmed)) {
            throwValidation("USERNAME_INVALID", "Invalid Username",
                    "Username cannot start or end with special characters");
        }
        if (CONSECUTIVE_SPECIALS_PATTERN.matcher(trimmed).find()) {
            throwValidation("USERNAME_INVALID", "Invalid Username",
                    "Username cannot contain consecutive special characters");
        }
    }

    /**
     * Validates and normalises a full name to title case.
     * Accepts names with two or more parts (e.g. "john doe", "Mary-Jane Watson").
     *
     * @param fullname the full name to validate
     * @return the normalised full name in title case
     * @throws ChatStackException if the full name does not meet requirements
     */
    public static String validateAndNormalizeFullname(String fullname) {
        if (StringUtils.isBlank(fullname)) {
            throwValidation("FULLNAME_REQUIRED", "Invalid Fullname", "Fullname is required");
        }

        String trimmed = fullname.trim().replaceAll("\\s+", " ");

        if (trimmed.length() > FULLNAME_MAX_LENGTH) {
            throwValidation("FULLNAME_INVALID", "Invalid Fullname",
                    "Fullname must not exceed " + FULLNAME_MAX_LENGTH + " characters");
        }

        String[] parts = trimmed.split(" ");

        if (parts.length < FULLNAME_MIN_PARTS) {
            throwValidation("FULLNAME_INVALID", "Invalid Fullname",
                    "Fullname must contain at least a first name and a last name");
        }

        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (!NAME_PART_PATTERN.matcher(parts[i]).matches()) {
                throwValidation("FULLNAME_INVALID", "Invalid Fullname",
                        "Name parts can only contain letters, hyphens, and apostrophes");
            }
            if (i > 0) {
                normalized.append(' ');
            }
            normalized.append(capitalizePart(parts[i]));
        }

        return normalized.toString();
    }

    private static boolean startsOrEndsWithSpecial(String value) {
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return isSpecialChar(first) || isSpecialChar(last);
    }

    private static boolean isSpecialChar(char c) {
        return c == '.' || c == '_' || c == '-';
    }

    private static String capitalizePart(String part) {
        StringBuilder sb = new StringBuilder(part.length());
        boolean capitalizeNext = true;
        for (char c : part.toCharArray()) {
            if (c == '-' || c == '\'') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static void throwValidation(String code, String reason, String description) {
        throw new ChatStackException(reason, code, description, HttpStatus.BAD_REQUEST);
    }

}

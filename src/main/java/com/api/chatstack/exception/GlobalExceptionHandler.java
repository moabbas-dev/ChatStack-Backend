package com.api.chatstack.exception;

import com.chatstack.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatStackException.class)
    public ResponseEntity<ErrorResponse> handleChatStackException(ChatStackException ex) {
        log.error("ChatStack exception: {} - {}", ex.getReason(), ex.getDescription());
        ErrorResponse error = buildErrorResponse(ex.getCode(), ex.getReason(), ex.getDescription());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("USER_NOT_FOUND", "User Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex) {
        log.error("Token expired: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("TOKEN_EXPIRED", "Token Expired", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(NoTokenProvidedException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(NoTokenProvidedException ex) {
        log.error("No Token Provided: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("NO_TOKEN_EXIST", "No Token Provided", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyVerifiedException(EmailAlreadyVerifiedException ex) {
        log.error("Email already verified: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("EMAIL_ALREADY_VERIFIED", "Email Already Verified", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnverifiedEmailException.class)
    public ResponseEntity<ErrorResponse> handleUnverifiedEmailException(UnverifiedEmailException ex) {
        log.error("Unverified email: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("UNVERIFIED_EMAIL", "Unverified Email", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmailException(InvalidEmailException ex) {
        log.error("Invalid email: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("INVALID_EMAIL", "Invalid Email", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException ex) {
        log.error("Invalid password: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("INVALID_PASSWORD", "Invalid Password", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidVerificationLinkException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationLinkException(InvalidVerificationLinkException ex) {
        log.error("Invalid verification link: {}", ex.getMessage());
        ErrorResponse error = buildErrorResponse("INVALID_VERIFICATION_LINK", "Invalid Verification Link", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException  ex) {
        String description = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse error = buildErrorResponse("VALIDATION_ERROR", description, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorResponse error = buildErrorResponse("MESSAGE_NOT_READABLE", "Message Not Readable", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse error = buildErrorResponse("INTERNAL_SERVER_ERROR", "Internal Server Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ErrorResponse buildErrorResponse(String code, String reason, String description) {
        return new ErrorResponse()
                .code(code)
                .reason(reason)
                .description(description);
    }
}

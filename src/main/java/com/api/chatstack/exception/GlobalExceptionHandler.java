package com.api.chatstack.exception;

import com.chatstack.dto.Error;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatStackException.class)
    public ResponseEntity<Error> handleChatStackException(ChatStackException ex) {
        log.error("ChatStack exception: {} - {}", ex.getReason(), ex.getDescription());
        Error error = new Error();
        error.setCode(ex.getCode());
        error.setDescription(ex.getDescription());
        error.setReason(ex.getReason());

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidationException(MethodArgumentNotValidException  ex) {
        String description = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        Error error = new Error();
        error.setReason("Invalid Format");
        error.setCode("VALIDATION_ERROR");
        error.setDescription(description);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        Error error = new Error();
                error.setReason("Internal Server Error");
                error.setCode("INTERNAL_ERROR");
                error.setDescription("An unexpected error occurred");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}

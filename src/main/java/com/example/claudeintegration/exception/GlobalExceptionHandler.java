package com.example.claudeintegration.exception;

import com.example.claudeintegration.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates exceptions thrown anywhere in the request pipeline into the
 * uniform {@link ErrorResponse} JSON shape, with the correct HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Bad Request", message, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Bad Request", "Malformed or missing JSON request body",
                        HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(ClaudeApiException.class)
    public ResponseEntity<ErrorResponse> handleClaudeApi(ClaudeApiException ex) {
        log.error("Claude API call failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Bad Gateway", ex.getMessage(), HttpStatus.BAD_GATEWAY.value()));
    }

    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<ErrorResponse> handleGeminiApi(GeminiApiException ex) {
        log.error("Gemini API call failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Bad Gateway", ex.getMessage(), HttpStatus.BAD_GATEWAY.value()));
    }

    @ExceptionHandler(OpenAiApiException.class)
    public ResponseEntity<ErrorResponse> handleOpenAiApi(OpenAiApiException ex) {
        log.error("OpenAI API call failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Bad Gateway", ex.getMessage(), HttpStatus.BAD_GATEWAY.value()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        log.error("Product database query failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Service Unavailable", "Product database is unavailable",
                        HttpStatus.SERVICE_UNAVAILABLE.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Internal Server Error", "An unexpected error occurred",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}

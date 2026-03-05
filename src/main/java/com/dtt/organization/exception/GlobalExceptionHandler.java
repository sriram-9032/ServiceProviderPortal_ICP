package com.dtt.organization.exception;


import com.dtt.organization.util.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String CLASS = "GlobalExceptionHandler";
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponses<Map<String, String>>> handleDtoValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        return ResponseEntity.badRequest().body(
                AppUtil.createApiResponses(false, "Validation failed", errors)
        );
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponses<Object>> handleBusinessValidation(
            ValidationException ex) {

        return ResponseEntity.badRequest().body(
                AppUtil.createApiResponses(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponses<Void>> handleException(
            Exception ex) {

        logger.error("{} Unhandled exception occurred", CLASS, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AppUtil.createApiResponses(false, "Something went wrong", null));
    }
}


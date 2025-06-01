package com.objectdetection.exception;

import com.objectdetection.model.DetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<DetectionResult> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File size exceeded maximum allowed size", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(DetectionResult.builder()
                        .error("File size exceeded maximum allowed size (10MB)")
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DetectionResult> handleValidationExceptions(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        log.error("Validation error: {}", errors);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(DetectionResult.builder()
                        .error("Validation error: " + errors)
                        .build());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<DetectionResult> handleIOException(IOException e) {
        log.error("IO error occurred", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DetectionResult.builder()
                        .error("Failed to process image: " + e.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DetectionResult> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DetectionResult.builder()
                        .error("An unexpected error occurred: " + e.getMessage())
                        .build());
    }
}
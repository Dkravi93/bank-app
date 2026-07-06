package com.deepakravi.bankapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> b = new HashMap<>();
        b.put("timestamp", Instant.now().toString());
        b.put("status", status.value());
        b.put("error", status.getReasonPhrase());
        b.put("message", message);
        return b;
    }

    @ExceptionHandler(ApiExceptions.ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ApiExceptions.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.InsufficientFundsException.class)
    public ResponseEntity<Object> handleInsufficientFunds(ApiExceptions.InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.DuplicateRequestException.class)
    public ResponseEntity<Object> handleDuplicate(ApiExceptions.DuplicateRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.AccountNotActiveException.class)
    public ResponseEntity<Object> handleAccountNotActive(ApiExceptions.AccountNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.EmailAlreadyExistsException.class)
    public ResponseEntity<Object> handleEmailExists(ApiExceptions.EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // Thrown by @Version when two transfers race past the pessimistic lock
    // (e.g. lock timeout) and collide on write.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, "Account was updated concurrently, please retry the request"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> b = body(HttpStatus.BAD_REQUEST, "Validation failed");
        b.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(b);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"));
    }
}

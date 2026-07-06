package com.deepakravi.bankapp.exception;

public class ApiExceptions {

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) { super(message); }
    }

    public static class DuplicateRequestException extends RuntimeException {
        public DuplicateRequestException(String message) { super(message); }
    }

    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException(String message) { super(message); }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) { super(message); }
    }
}

package com.example.realestate_backend.exception;

public class DuplicateEmailException extends IllegalArgumentException {
    public DuplicateEmailException() {
        super("An account with this email already exists.");
    }
}

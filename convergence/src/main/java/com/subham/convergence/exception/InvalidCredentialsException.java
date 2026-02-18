package com.subham.convergence.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message){
        super("Invalid credentials provided: " + message);
    }
    
}

package com.subham.convergence.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message){
        super("User name " + message + " was not found: please register first.");
    }
}

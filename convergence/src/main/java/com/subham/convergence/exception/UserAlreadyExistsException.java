package com.subham.convergence.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message){
        super("User name " + message + " already exists: please choose a different username.");
    }
    
}

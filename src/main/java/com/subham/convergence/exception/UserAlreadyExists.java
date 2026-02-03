package com.subham.convergence.exception;

public class UserAlreadyExists exceeds RuntimeException {
    public UserAlreadyExists(String message){
        super("User name " + message + " already exists: please choose a different username.");
    }
    
}

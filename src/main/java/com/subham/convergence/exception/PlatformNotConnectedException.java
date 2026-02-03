package com.subham.convergence.exception;

public class PlatformNotConnectedException extends RuntimeException {
    public PlatformNotConnectedException(String message){
        super("Platform " + message + " is not connected.");
    }
    
}

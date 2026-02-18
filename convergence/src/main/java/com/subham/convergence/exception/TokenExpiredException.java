package com.subham.convergence.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message){
        super("Token has expired: " + message);
    }

}

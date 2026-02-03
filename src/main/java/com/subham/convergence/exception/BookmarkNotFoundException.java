package com.subham.convergence.exception;

public class BookmarkNotFoundException extends RuntimeException {
    public BookmarkNotFoundException(String message){
        super("Bookmark with ID " + message + " was not found.");
    }
}

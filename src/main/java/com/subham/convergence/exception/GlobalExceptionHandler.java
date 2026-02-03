package com.subham.convergence.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler{
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException
    (MethodArgumentNotValidException ex,WebRequest request)
    {
        Map<String,String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
        .forEach(error-> fieldErrors.put
                         (error.getField(),
                         error.getDefaultMessage()
                         )
                );
        Map<String, String> response = new HashMap<>();
        response.put("status",HttpStatus.BAD_REQUEST.value());
        response.put("errors",fieldErrors);
        response.put("timestamp",LocalDateTime.now());
        response.put("path",request.getDescription(false).replace("uri=",""));

        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException
    (UserNotFoundException ex, WebRequest request){
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND,"User Not Found, register first", request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException
    (UserAlreadyExistsException ex, WebRequest request){
        return buildErrorResponse(ex, HttpStatus.CONFLICT,"User Already Exists, choose different username", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException
    (InvalidCredentialsException ex, WebRequest request){
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED,"Invalid Credentials", request);
    }

    @ExceptionHandler(BookmarkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookmarkNotFoundException
    (BookmarkNotFoundException ex, WebRequest request){
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND,"Bookmark Not Found", request);
    }

    @ExceptionHandler(PlatformNotConnectedException.class)
    public ResponseEntity<ErrorResponse> handlePlatformNotConnectedException
    (PlatformNotConnectedException ex, WebRequest request){
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST,"Platform Not Connected", request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException
    (TokenExpiredException ex, WebRequest request){
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED,"Token Expired", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex, WebRequest request)
        {
            return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred", request);
        }

    //Generic Error Response Builder
    private ResponseEntity<ErrorResponse> buildErrorResponse(
        Exception ex, HttpStatus status, String errorMessage, WebRequest request){
            ErrorResponse errorResponse = ErrorResponse.builder()
            .status(status.value())
            .error(errorMessage)
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=",""))
            .timestamp(LocalDateTime.now())
            .build();

            return new ResponseEntity<>(errorResponse, status);
    }
}




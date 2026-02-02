package com.subham.convergence.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.subham.convergence.validation.ValidUsername; 

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {
    
    private int min;
    private int max;
    private String minMessage;
    private String maxMessage;

    @Override
    public void initialize(ValidUsername annotation) {
        min = annotation.min();
        max = annotation.max();
        minMessage = annotation.minMessage();
        maxMessage = annotation.maxMessage();
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        
        if (username == null) {
            return true;
        }
        
        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (username.length() < min) {
            String message = minMessage
                .replace("{value}", username)
                .replace("{min}", String.valueOf(min));
            
            context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
            isValid = false;
        }

        if (username.length() > max) {
            String message = maxMessage
                .replace("{value}", username)
                .replace("{max}", String.valueOf(max));
            
            context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
            isValid = false;
        }
        
        return isValid;
    }
}



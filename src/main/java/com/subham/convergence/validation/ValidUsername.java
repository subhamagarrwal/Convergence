package com.subham.convergence.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UsernameValidator.class)

public @interface ValidUsername {
    String message() default "Invalid username";

    Class<?>[] groups() default {};
    
    Class<? extends Payload> [] payload() default {};

    int min() default 8;
    int max() default 24;
    String minMessage() default "Username {value} must be at least {min} characters long";
    String maxMessage() default "Username {value} must be at most {max} characters long";
}

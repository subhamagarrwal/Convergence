package com.subham.convergence.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is mandatory")
    private String email;
    
    @NotBlank(message = "Password needed")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    
}

package com.subham.convergence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private String username;
    private String message;
}

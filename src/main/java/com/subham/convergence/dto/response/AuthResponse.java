package com.subham.convergence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // ← ADD THIS!
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private String username;
    private String message;
}

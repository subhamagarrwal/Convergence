// filepath: c:\Users\subha\Desktop\Projects\Convergence\convergence\src\main\java\com\subham\convergence\dto\response\AuthResponse.java
package com.subham.convergence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String userId;
    private String email;
    private String username;
    private String message;
}
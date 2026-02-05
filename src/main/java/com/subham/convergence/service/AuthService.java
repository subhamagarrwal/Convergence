package com.subham.convergence.service;

import com.subham.convergence.dto.request.LoginRequest;
import com.subham.convergence.dto.request.RegisterRequest;
import com.subham.convergence.dto.response.AuthResponse;
import com.subham.convergence.exception.InvalidCredentialsException;
import com.subham.convergence.exception.UserAlreadyExistsException;
import com.subham.convergence.model.User;
import com.subham.convergence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        // Generate JWT tokens
        String token = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .username(user.getUsername())
                .message("Registration successful")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate tokens
        String token = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .username(user.getUsername())
                .message("Login successful")
                .build();
    }
}

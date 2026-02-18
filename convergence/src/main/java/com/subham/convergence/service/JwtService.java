package com.subham.convergence.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private static final Long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, jwtExpiration);
    }

    public String generateRefreshToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, REFRESH_TOKEN_EXPIRATION);
    }


    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)                                          // ← NEW: claims() instead of setClaims()
                .subject(subject)                                        // ← NEW: subject() instead of setSubject()
                .issuedAt(new Date(System.currentTimeMillis()))          // ← NEW: issuedAt() instead of setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + expiration))  // ← NEW: expiration() instead of setExpiration()
                .signWith(getSignInKey(),Jwts.SIG.HS256)                               // ← NEW: Only one parameter (auto-detects HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()                    // ← NEW: parser() instead of parserBuilder()
                .verifyWith(getSignInKey())    // ← NEW: verifyWith() instead of setSigningKey()
                .build()
                .parseSignedClaims(token)       // ← NEW: parseSignedClaims() instead of parseClaimsJws()
                .getPayload();                  // ← NEW: getPayload() instead of getBody()
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
}
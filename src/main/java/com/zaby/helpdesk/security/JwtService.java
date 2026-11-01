package com.zaby.helpdesk.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret:dGhpcy1pcy1hLXNlY3VyZS0yNTYtYml0LXNlY3JldC1rZXktZm9yLWhlbHBkZXNrLWFwcA==}")
    private String secret;

    @Value("${jwt.expiration-ms:900000}")
    private long expirationMs;

    private Key getSignKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate unique access token for each login
    public String generateAccessToken(UserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getUsername()) // email or username
                .setId(UUID.randomUUID().toString())   // ensures uniqueness
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract email/username from token
    public String extractEmail(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Validate token against UserDetails
    public boolean isTokenValid(String token, UserDetails userDetails){
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Check if token is expired
    private boolean isTokenExpired(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }
}

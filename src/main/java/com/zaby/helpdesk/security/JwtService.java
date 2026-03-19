package com.zaby.helpdesk.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final String SECRET = "mysecretkeymysecretkeymysecretkey123";

    private Key getSignKey(){
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Generate unique access token for each login
    public String generateAccessToken(UserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getUsername()) // email or username
                .setId(UUID.randomUUID().toString())   // ensures uniqueness
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 900_000)) // 15 min
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
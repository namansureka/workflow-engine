package com.naman.workflow_engine.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
    public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
        private final long EXPIRATION_MS = 86400000; // 24 hours

        private SecretKey getSigningKey() {
            return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        }

            public String generateToken(String username, String role) {
                return Jwts.builder()
                        .setSubject(username)
                        .claim("role", role)
                        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                        .signWith(getSigningKey())
                        .compact();
            }

            public Claims validateToken(String token) {
                return Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            }
        public String extractUsername(String token) {
            return validateToken(token).getSubject();
    }

        public String extractRole(String token) {
             return validateToken(token).get("role", String.class);
    }
}

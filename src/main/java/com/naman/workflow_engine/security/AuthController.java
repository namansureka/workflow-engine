package com.naman.workflow_engine.security;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {

        if (request.getUsername().equals("admin") && request.getPassword().equals("admin123")) {
            String token = jwtUtil.generateToken(request.getUsername(), "ADMIN");
            return Map.of("token", token);
        } else if (request.getUsername().equals("viewer") && request.getPassword().equals("viewer123")) {
            String token = jwtUtil.generateToken(request.getUsername(), "VIEWER");
            return Map.of("token", token);
        }
        throw new IllegalArgumentException("Invalid credentials");
    }
}

package com.carsharing.controllers;

import com.carsharing.entities.User;
import com.carsharing.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // In-memory token storage (in production, use Redis or database)
    private final Map<String, AuthToken> tokenStore = new HashMap<>();

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        instance = this;
    }

    /**
     * Register a new user with authentication
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRegistrationRequest request) {
        try {
            User user = userService.registerUser(
                request.email(),
                request.phone(),
                request.firstName(),
                request.lastName(),
                request.password()
            );
            
            // Generate auth token
            String token = generateToken(user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Login user and get authentication token
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequest request) {
        try {
            var userOptional = userService.authenticateUser(request.email(), request.password());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                String token = generateToken(user.getId());
                return ResponseEntity.ok(new AuthResponse(token, user));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Logout user (invalidate token)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token != null && tokenStore.containsKey(token)) {
                tokenStore.remove(token);
            }
            return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Validate token and get user info
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token == null || !tokenStore.containsKey(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid or expired token"));
            }
            
            AuthToken authToken = tokenStore.get(token);
            if (authToken.isExpired()) {
                tokenStore.remove(token);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token expired"));
            }
            
            var userOptional = userService.getUserById(authToken.getUserId());
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(userOptional.get());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Refresh token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String oldToken = extractToken(authHeader);
            if (oldToken == null || !tokenStore.containsKey(oldToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            AuthToken authToken = tokenStore.get(oldToken);
            if (authToken.isExpired()) {
                tokenStore.remove(oldToken);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token expired"));
            }
            
            // Generate new token
            String newToken = generateToken(authToken.getUserId());
            tokenStore.remove(oldToken);
            
            var userOptional = userService.getUserById(authToken.getUserId());
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(new AuthResponse(newToken, userOptional.get()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Helper methods

    public String generateToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        AuthToken authToken = new AuthToken(token, userId, LocalDateTime.now().plusHours(24));
        tokenStore.put(token, authToken);
        return token;
    }
    
    public boolean isValidToken(String token) {
        if (token == null || !tokenStore.containsKey(token)) {
            return false;
        }
        return !tokenStore.get(token).isExpired();
    }
    
    public UUID getUserIdFromToken(String token) {
        if (token == null || !tokenStore.containsKey(token)) {
            return null;
        }
        return tokenStore.get(token).getUserId();
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // Request/Response records

    public record AuthRegistrationRequest(
            String email,
            String phone,
            String firstName,
            String lastName,
            String password
    ) {}

    public record AuthLoginRequest(String email, String password) {}

    public record AuthResponse(String token, User user) {}

    public record ErrorResponse(String message) {}

    public record MessageResponse(String message) {}

    // Token storage class
    private static class AuthToken {
        private final String token;
        private final UUID userId;
        private final LocalDateTime expiresAt;

        public AuthToken(String token, UUID userId, LocalDateTime expiresAt) {
            this.token = token;
            this.userId = userId;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public UUID getUserId() { return userId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
    
    // Static reference for JWT filter access
    private static AuthController instance;
    
    public static AuthController getInstance() {
        return instance;
    }
}
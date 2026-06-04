package com.carsharing.controllers;

import com.carsharing.entities.User;
import com.carsharing.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user
     * POST /api/users/register
     * Validates: Requirements 1.1
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        logger.info("User registration attempt: email={}", request.email());
        try {
            User user = userService.registerUser(
                request.email(),
                request.phone(),
                request.firstName(),
                request.lastName(),
                request.password()
            );
            logger.info("User registered successfully: id={}, email={}", user.getId(), user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            logger.warn("User registration failed: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Authenticate user
     * POST /api/users/login
     * Validates: Requirements 1.3
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        logger.info("User login attempt: email={}", request.email());
        try {
            var userOptional = userService.authenticateUser(request.email(), request.password());
            if (userOptional.isPresent()) {
                logger.info("User logged in successfully: id={}, email={}", userOptional.get().getId(), request.email());
                return ResponseEntity.ok(userOptional.get());
            } else {
                logger.warn("User login failed: invalid credentials for email={}", request.email());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
            }
        } catch (Exception e) {
            logger.error("User login error: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        logger.info("Get user by ID: id={}", id);
        try {
            var userOptional = userService.getUserById(id);
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(userOptional.get());
            } else {
                logger.warn("User not found: id={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("User not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting user: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update user profile
     * PUT /api/users/{id}/profile
     * Validates: Requirements 1.4
     */
    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable UUID id,
            @RequestBody ProfileUpdateRequest request) {
        logger.info("Update user profile: id={}, firstName={}, lastName={}", id, request.firstName(), request.lastName());
        try {
            User user = userService.updateUserProfile(
                id,
                request.firstName(),
                request.lastName(),
                request.phone()
            );
            logger.info("User profile updated successfully: id={}", id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            logger.warn("User profile update failed (not found): id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("User profile update failed (deleted user): id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Soft delete user account
     * DELETE /api/users/{id}
     * Validates: Requirements 1.2
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        logger.info("Delete user requested: id={}", id);
        try {
            userService.deleteUser(id);
            logger.info("User marked as deleted: id={}", id);
            return ResponseEntity.ok(new MessageResponse("User marked as deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("User deletion failed (not found): id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Change user password
     * PUT /api/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable UUID id,
            @RequestBody PasswordChangeRequest request) {
        logger.info("Change password requested: id={}", id);
        try {
            userService.changePassword(id, request.currentPassword(), request.newPassword());
            logger.info("Password changed successfully: id={}", id);
            return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Password change failed: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Password change failed (deleted user): id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all active users
     * GET /api/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        logger.info("Get all active users requested");
        List<User> activeUsers = userService.getAllActiveUsers();
        logger.info("Retrieved {} active users", activeUsers.size());
        return ResponseEntity.ok(activeUsers);
    }

    // Request/Response records

    public record UserRegistrationRequest(
            String email,
            String phone,
            String firstName,
            String lastName,
            String password
    ) {}

    public record LoginRequest(String email, String password) {}

    public record ProfileUpdateRequest(String firstName, String lastName, String phone) {}

    public record PasswordChangeRequest(String currentPassword, String newPassword) {}

    public record ErrorResponse(String message) {}

    public record MessageResponse(String message) {}
}
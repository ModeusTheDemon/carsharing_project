package com.carsharing.services;

import com.carsharing.entities.User;
import com.carsharing.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String email, String phone, String firstName, String lastName, String password) {
        logger.debug("Registering user: email={}, firstName={}, lastName={}", email, firstName, lastName);
        if (userRepository.existsByEmail(email)) {
            logger.warn("Email already registered: email={}", email);
            throw new IllegalArgumentException("Email already registered");
        }
        
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email validation failed: email is empty");
            throw new IllegalArgumentException("Email is required");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            logger.warn("First name validation failed: first name is empty");
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            logger.warn("Last name validation failed: last name is empty");
            throw new IllegalArgumentException("Last name is required");
        }
        if (password == null || password.trim().isEmpty()) {
            logger.warn("Password validation failed: password is empty");
            throw new IllegalArgumentException("Password is required");
        }
        
        String hashedPassword = passwordEncoder.encode(password);
        
        User user = new User(email, phone, firstName, lastName, hashedPassword);
        logger.info("User registered: id={}, email={}", user.getId(), user.getEmail());
        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(String email, String password) {
        logger.debug("Authenticating user: email={}", email);
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (Boolean.TRUE.equals(user.getIsDeleted())) {
                logger.warn("Login attempt for deleted user: email={}", email);
                return Optional.empty();
            }
            
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                logger.info("User authenticated: id={}, email={}", user.getId(), email);
                return Optional.of(user);
            } else {
                logger.warn("Invalid password for user: email={}", email);
            }
        } else {
            logger.warn("User not found for authentication: email={}", email);
        }
        
        return Optional.empty();
    }

    @Transactional
    public void deleteUser(UUID userId) {
        logger.info("Deleting user: id={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found for deletion: id={}", userId);
                    return new IllegalArgumentException("User not found");
                });
        
        user.setIsDeleted(true);
        userRepository.save(user);
        logger.info("User marked as deleted: id={}", userId);
    }

    @Transactional
    public User updateUserProfile(UUID userId, String firstName, String lastName, String phone) {
        logger.info("Updating user profile: id={}, firstName={}, lastName={}", userId, firstName, lastName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found for profile update: id={}", userId);
                    throw new IllegalArgumentException("User not found");
                });
        
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            logger.warn("Cannot update profile for deleted user: id={}", userId);
            throw new IllegalStateException("Cannot update deleted user");
        }
        
        user.updateProfile(firstName, lastName, phone);
        logger.info("User profile updated: id={}", userId);
        return userRepository.save(user);
    }

    public Optional<User> getUserById(UUID userId) {
        logger.debug("Getting user by ID: id={}", userId);
        return userRepository.findById(userId)
                .filter(user -> {
                    boolean isActive = !Boolean.TRUE.equals(user.getIsDeleted());
                    if (!isActive) {
                        logger.debug("User is deleted: id={}", userId);
                    }
                    return isActive;
                });
    }

    public Optional<User> getUserByEmail(String email) {
        logger.debug("Getting user by email: email={}", email);
        return userRepository.findByEmail(email)
                .filter(user -> {
                    boolean isActive = !Boolean.TRUE.equals(user.getIsDeleted());
                    if (!isActive) {
                        logger.debug("User is deleted: email={}", email);
                    }
                    return isActive;
                });
    }

    public List<User> getAllActiveUsers() {
        logger.debug("Getting all active users");
        List<User> activeUsers = userRepository.findByIsDeleted(false);
        logger.info("Retrieved {} active users", activeUsers.size());
        return activeUsers;
    }

    public List<User> getAllDeletedUsers() {
        logger.debug("Getting all deleted users");
        List<User> deletedUsers = userRepository.findByIsDeleted(true);
        logger.info("Retrieved {} deleted users", deletedUsers.size());
        return deletedUsers;
    }

    public boolean isUserActive(UUID userId) {
        logger.debug("Checking if user is active: id={}", userId);
        boolean isActive = userRepository.findById(userId)
                .map(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .orElse(false);
        if (isActive) {
            logger.debug("User is active: id={}", userId);
        } else {
            logger.debug("User is not active: id={}", userId);
        }
        return isActive;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        logger.info("Changing password for user: id={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found for password change: id={}", userId);
                    throw new IllegalArgumentException("User not found");
                });
        
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            logger.warn("Cannot change password for deleted user: id={}", userId);
            throw new IllegalStateException("Cannot change password for deleted user");
        }
        
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            logger.warn("Current password is incorrect for user: id={}", userId);
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Password changed successfully for user: id={}", userId);
    }
}
package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.UserRepository;
// It's better to use a dedicated service for user creation and password encoding
// import com.LostAndFound.LostAndFound.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Consider using Spring Security for password encoding
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // For validating the User object if annotations are added
import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // Import for Optional

import lombok.extern.slf4j.Slf4j; // <<< ADD THIS IMPORT FOR @Slf4j

@Slf4j // <<< ADD THIS LOMBOK ANNOTATION FOR LOGGING
@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*") // Configure properly for production
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private PasswordEncoder passwordEncoder; // For password hashing (recommended)

    // @Autowired
    // private UserService userService; // Better to delegate user creation to a service

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User registrationRequest) {
        log.info("Registration attempt for email: {}", registrationRequest.getEmail());
        // Basic validation
        if (registrationRequest.getUsername() == null || registrationRequest.getUsername().trim().isEmpty()) {
            log.warn("Registration failed: Username is required.");
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        if (registrationRequest.getEmail() == null || registrationRequest.getEmail().trim().isEmpty()) {
            log.warn("Registration failed: Email is required.");
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (registrationRequest.getPassword() == null || registrationRequest.getPassword().trim().isEmpty()) {
            log.warn("Registration failed: Password is required for email {}.", registrationRequest.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }
        if (registrationRequest.getPassword().length() < 6) {
            log.warn("Registration failed: Password too short for email {}.", registrationRequest.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters long."));
        }

        String emailToRegister = registrationRequest.getEmail().trim().toLowerCase();
        String usernameToRegister = registrationRequest.getUsername().trim();

        // Check if email exists
        if (userRepository.findByEmail(emailToRegister).isPresent()) {
            log.warn("Registration failed: Email {} already registered.", emailToRegister);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email is already registered."));
        }
        // Check if username exists
        if (userRepository.findByUsername(usernameToRegister).isPresent()) { // Add findByUsername to UserRepository if not present
            log.warn("Registration failed: Username {} already taken.", usernameToRegister);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username is already taken."));
        }

        User newUser = new User();
        newUser.setUsername(usernameToRegister);
        newUser.setEmail(emailToRegister);

        // IMPORTANT: HASH THE PASSWORD before saving! This is critical for security.
        // Example with Spring Security (uncomment PasswordEncoder autowiring):
        // newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        newUser.setPassword(registrationRequest.getPassword()); // Storing plain text - NOT SECURE FOR PRODUCTION

        String requestedRole = registrationRequest.getRole();
        if ("admin".equalsIgnoreCase(requestedRole)) {
            // SECURITY WARNING: Allowing open admin registration is dangerous for production.
            newUser.setRole("admin");
            newUser.setVerified(true); // Admins might be auto-verified
            log.warn("ADMIN account created via public registration: {} ({})", newUser.getUsername(), newUser.getEmail());
        } else {
            newUser.setRole("user"); // Default to user
            newUser.setVerified(false); // Users might need email verification later
        }

        User savedUser = userRepository.save(newUser);
        log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful!");
        // Optionally return some user data (but not password)
        // response.put("userId", savedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        log.info("Login attempt for email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            log.warn("Login failed: Email is required.");
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (password == null || password.trim().isEmpty()) {
            log.warn("Login failed: Password is required for email {}.", email);
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());

        if (userOptional.isEmpty()) {
            log.warn("Login failed: No user found for email {}.", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }

        User user = userOptional.get();

        // IMPORTANT: Compare HASHED PASSWORDS in a real app
        // if (!passwordEncoder.matches(password, user.getPassword())) {
        if (!user.getPassword().equals(password)) { // Current plain text comparison - NOT SECURE
            log.warn("Login failed: Invalid password for email {}.", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }

        log.info("User {} (ID: {}) logged in successfully.", user.getUsername(), user.getId());
        // Successful login
        // TODO: Generate and return a JWT token here for proper session management.
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", user.getId());
        responseBody.put("username", user.getUsername());
        responseBody.put("email", user.getEmail());
        responseBody.put("role", user.getRole());
        // responseBody.put("token", "your_generated_jwt_token_here");

        return ResponseEntity.ok(responseBody);
    }
}

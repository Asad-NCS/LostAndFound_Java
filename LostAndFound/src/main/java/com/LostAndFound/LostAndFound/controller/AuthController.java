package com.LostAndFound.LostAndFound.controller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import java.util.Map; 

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final UserRepository userRepository;

    // Constructor injection
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login") // lowercase 'login' to match REST conventions
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        // 1. Find user by email
        User user = userRepository.findByEmail(credentials.get("email"));

        // 2. Check if user exists
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        // 3. Plain text password comparison
        if (!credentials.get("password").equals(user.getPassword())) {
            return ResponseEntity.status(401).body("Wrong password");
        }

        // 4. Return user data on success
        return ResponseEntity.ok(user);
    }
}
package com.LostAndFound.LostAndFound.test; // Assuming this is the correct package for your file

import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import lombok.RequiredArgsConstructor; // Added for constructor injection
import lombok.extern.slf4j.Slf4j;   // Added for logging
import org.springframework.stereotype.Service;
// import org.springframework.beans.factory.annotation.Autowired; // Using constructor injection now

import java.util.Optional; // Import for Optional

@Slf4j // For logging
@Service
@RequiredArgsConstructor // Replaces @Autowired for constructor injection (recommended)
public class AuthenticationService {

    // UserRepository will be injected via the constructor by Lombok's @RequiredArgsConstructor
    private final UserRepository userRepository;

    // Authenticate with plain text (not recommended for production!)
    public boolean authenticate(String email, String rawPassword) {
        log.info("Authentication attempt for email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            log.warn("Authentication failed: Email was null or empty.");
            return false;
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            log.warn("Authentication failed: Password was null or empty for email: {}", email);
            return false;
        }

        Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());

        if (userOptional.isEmpty()) {
            log.warn("Authentication failed: User not found for email: {}", email);
            // System.out.println("User not found for email: " + email); // Your original log
            return false;
        }

        User user = userOptional.get(); // Get the User object from Optional

        // System.out.println("Raw password entered: " + rawPassword); // Your original log
        // System.out.println("Password from DB: " + user.getPassword()); // Your original log

        // IMPORTANT: This is plain text password comparison.
        // In a real application, you MUST hash passwords during registration
        // and use a method like passwordEncoder.matches(rawPassword, user.getPassword()) here.
        if (rawPassword.equals(user.getPassword())) {
            log.info("User {} authenticated successfully.", user.getUsername());
            // System.out.println("Password matches!"); // Your original log
            return true;
        } else {
            log.warn("Authentication failed: Invalid password for email {}.", email);
            // System.out.println("Password does not match!"); // Your original log
            return false;
        }
    }

    // Example: If you need to fetch the user object after authentication,
    // you might have another method or return User from authenticate.
    public Optional<User> getAuthenticatedUser(String email, String rawPassword) {
        if (authenticate(email, rawPassword)) {
            return userRepository.findByEmail(email.trim().toLowerCase());
        }
        return Optional.empty();
    }
}

package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.UserDTO;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional; // Make sure this is imported
import java.util.stream.Collectors;

@Slf4j // Added for logging
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    // @Autowired // Recommended to inject via constructor if using Spring Security
    // private PasswordEncoder passwordEncoder; // For password hashing

    @Transactional // Good practice for methods that modify data
    public UserDTO createUser(UserDTO userDTO) {
        log.info("Attempting to create user with email: {}", userDTO.getEmail());
        // Check if email or username already exists
        if (userRepository.findByEmail(userDTO.getEmail().trim().toLowerCase()).isPresent()) {
            log.warn("User creation failed: Email {} already exists.", userDTO.getEmail());
            throw new IllegalArgumentException("Email already exists.");
        }
        if (userRepository.findByUsername(userDTO.getUsername().trim()).isPresent()) {
            log.warn("User creation failed: Username {} already exists.", userDTO.getUsername());
            throw new IllegalArgumentException("Username already exists.");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername().trim());
        user.setEmail(userDTO.getEmail().trim().toLowerCase());
        // IMPORTANT: Hash the password before saving!
        // user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setPassword(userDTO.getPassword()); // Storing plain text - NOT SECURE FOR PRODUCTION

        // Set role from DTO if provided, otherwise default in User model will apply
        if (userDTO.getRole() != null && !userDTO.getRole().isBlank()) {
            if ("admin".equalsIgnoreCase(userDTO.getRole())) {
                user.setRole("admin");
                user.setVerified(true); // Admins might be auto-verified
                log.info("Creating admin user: {}", user.getUsername());
            } else {
                user.setRole("user");
            }
        } // Else, relies on User model's default role

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return toDTO(savedUser); // Ensure password is not returned in DTO
    }

    public UserDTO getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .map(this::toDTO) // Ensure password is not returned in DTO
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new RuntimeException("User not found with ID: " + id);
                });
    }

    public List<UserDTO> getAllUsers() {
        log.debug("Fetching all users.");
        return userRepository.findAll().stream()
                .map(this::toDTO) // Ensure password is not returned in DTO
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("Attempting to update user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User update failed: User not found with ID: {}", id);
                    return new RuntimeException("User not found with ID: " + id);
                });

        // Check for email conflict if email is being changed
        if (userDTO.getEmail() != null && !userDTO.getEmail().trim().equalsIgnoreCase(user.getEmail())) {
            String newEmail = userDTO.getEmail().trim().toLowerCase();
            if (userRepository.findByEmail(newEmail).filter(existingUser -> !existingUser.getId().equals(id)).isPresent()) {
                log.warn("User update failed: New email {} already exists for user ID: {}", newEmail, id);
                throw new IllegalArgumentException("Email already in use by another account.");
            }
            user.setEmail(newEmail);
        }

        // Check for username conflict if username is being changed
        if (userDTO.getUsername() != null && !userDTO.getUsername().trim().equalsIgnoreCase(user.getUsername())) {
            String newUsername = userDTO.getUsername().trim();
            if (userRepository.findByUsername(newUsername).filter(existingUser -> !existingUser.getId().equals(id)).isPresent()) {
                log.warn("User update failed: New username {} already exists for user ID: {}", newUsername, id);
                throw new IllegalArgumentException("Username already taken.");
            }
            user.setUsername(newUsername);
        }


        // Only update password if a new one is provided and is not blank
        if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
            // IMPORTANT: Hash the password
            // user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            user.setPassword(userDTO.getPassword()); // Storing plain text - NOT SECURE
            log.info("Password updated for user ID: {}", id);
        }
        // Update role if provided (careful with allowing role changes via general update)
        if (userDTO.getRole() != null && !userDTO.getRole().isBlank()) {
            // Add validation or specific logic for role changes if needed
            user.setRole(userDTO.getRole());
            log.info("Role updated to {} for user ID: {}", userDTO.getRole(), id);
        }


        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {} (ID: {})", updatedUser.getUsername(), updatedUser.getId());
        return toDTO(updatedUser); // Ensure password is not returned in DTO
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Attempting to delete user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("User deletion failed: User not found with ID: {}", id);
            throw new RuntimeException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }

    // This is the loginUser method that was causing the error (around line 58)
    public UserDTO loginUser(String email, String password) {
        log.info("Login attempt for email: {}", email);
        Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());

        if (userOptional.isEmpty()) {
            log.warn("Login failed: No user found for email {}.", email);
            throw new RuntimeException("Invalid email or password.");
        }

        User user = userOptional.get();

        // IMPORTANT: Compare HASHED PASSWORDS in a real app
        // if (!passwordEncoder.matches(password, user.getPassword())) {
        if (!user.getPassword().equals(password)) { // Current plain text comparison - NOT SECURE
            log.warn("Login failed: Invalid password for email {}.", email);
            throw new RuntimeException("Invalid email or password.");
        }

        log.info("User {} (ID: {}) logged in successfully.", user.getUsername(), user.getId());
        return toDTO(user); // Ensure password is not returned in DTO
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone()); // Assuming UserDTO has phone
        dto.setRole(user.getRole());   // Assuming UserDTO has role
        // DO NOT set password in DTO returned to client
        // dto.setPassword(user.getPassword()); // REMOVE THIS IF IT WAS EVER THERE
        return dto;
    }
}

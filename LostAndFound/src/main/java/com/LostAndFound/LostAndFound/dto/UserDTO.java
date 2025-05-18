package com.LostAndFound.LostAndFound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size; // For password size, if you add it
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Password is included for registration/update requests,
    // but should NOT be included in responses from the server.
    // The toDTO method in UserService handles not sending it back.
    @NotBlank(message = "Password is required")
    // @Size(min = 6, message = "Password must be at least 6 characters") // Optional: add validation
    private String password;

    private String phone; // Added phone field

    private String role;  // Added role field
}

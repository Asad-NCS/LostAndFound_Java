package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor; // Added for consistency if other constructors are used

import java.util.List;

@Entity
@Table(name = "users") // Explicitly naming the table can be good practice
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor // For the constructor with all fields
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_verified", nullable = false) // Explicit column name
    private boolean verified = false;

    @Column(nullable = false)
    private String role = "user"; // Default role

    @Column // Added phone field to the entity
    private String phone;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Claim> claims;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> sentMessages;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> receivedMessages;

    // Removed Post and Report relationships as they were not in your previous User model
    // @OneToMany(mappedBy = "user")
    // private List<Post> posts;

    // @OneToMany(mappedBy = "user") // Assuming Report has a 'user' field for the reporter
    // private List<Report> reportedBy;


    // Verification relationship might be one-to-one if a user has one verification record
    // If a user can have multiple verification attempts/records, it would be OneToMany
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Verification verification;


    // Simplified constructor for registration (role defaults to "user", verified to false)
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        // this.verified and this.role will use defaults
    }

    // Constructor for admin creation or when role is explicitly set
    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.verified = "admin".equalsIgnoreCase(role); // Admins are auto-verified
    }

    // Note: The all-args constructor from @AllArgsConstructor will include 'phone'
    // The existing constructor User(Long id, String username, ..., Verification verification)
    // from your previous code was very long and might be better handled by @AllArgsConstructor
    // or specific constructors as needed. I've removed it for clarity as Lombok provides one.
    // If you need that specific long constructor, ensure 'phone' is added to its parameters.
}

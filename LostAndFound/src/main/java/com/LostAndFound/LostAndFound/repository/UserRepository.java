package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // Change return type to Optional
    Optional<User> findByUsername(String username); // Add this method
}
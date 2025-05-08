package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Custom method to find a user by email
    User findByEmail(String email);
}

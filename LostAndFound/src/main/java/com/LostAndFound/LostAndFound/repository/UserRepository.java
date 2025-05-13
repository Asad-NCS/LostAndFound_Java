package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email); // Add this method
}

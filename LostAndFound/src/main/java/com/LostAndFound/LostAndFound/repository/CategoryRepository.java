package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Add this method
    Optional<Category> findByNameIgnoreCase(String name);
}

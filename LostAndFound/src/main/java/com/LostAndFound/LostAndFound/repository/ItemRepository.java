package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByImagePath(String imagePath);
}
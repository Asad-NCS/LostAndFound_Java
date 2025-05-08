package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
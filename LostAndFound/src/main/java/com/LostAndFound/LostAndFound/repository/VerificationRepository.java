package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {
}
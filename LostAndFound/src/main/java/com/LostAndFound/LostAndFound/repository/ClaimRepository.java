package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Claim;
import com.LostAndFound.LostAndFound.model.ClaimStatus; // Import ClaimStatus
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByItemId(Long itemId);
    List<Claim> findByUserId(Long userId);
    // List<Claim> findByIsApproved(boolean isApproved); // Replaced by status
    List<Claim> findByStatus(ClaimStatus status); // Find by specific status
    List<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status); // Find by item and status
    boolean existsByUserIdAndItemIdAndStatus(Long userId, Long itemId, ClaimStatus status); // Check for existing pending claims
    // List<Claim> findByItemIdAndIsApproved(Long itemId, boolean isApproved); // Replaced by status
}
package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    // Find verification by claim ID
    Optional<Verification> findByClaimId(Long claimId);

    // Find verification by code and claim ID
    Optional<Verification> findByCodeAndClaimId(String code, Long claimId);

    // Check if verification exists and is verified for a claim
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
            "FROM Verification v WHERE v.claim.id = :claimId AND v.isVerified = true")
    boolean existsByClaimIdAndVerified(@Param("claimId") Long claimId);

    // Find all verifications for a specific user
    @Query("SELECT v FROM Verification v WHERE v.user.id = :userId")
    List<Verification> findAllByUserId(@Param("userId") Long userId);

    // Find all unverified verifications
    List<Verification> findByIsVerifiedFalse();

    // Find verification by claim ID with user and claim data loaded
    @Query("SELECT v FROM Verification v JOIN FETCH v.user JOIN FETCH v.claim WHERE v.claim.id = :claimId")
    Optional<Verification> findByClaimIdWithDetails(@Param("claimId") Long claimId);
}
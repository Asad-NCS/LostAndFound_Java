package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.VerificationDTO;
import com.LostAndFound.LostAndFound.model.Claim;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.model.Verification;
import com.LostAndFound.LostAndFound.repository.ClaimRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import com.LostAndFound.LostAndFound.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    public VerificationDTO requestVerification(Long claimId, Long userId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        User claimant = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate random 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        Verification verification = new Verification();
        verification.setCode(code);
        verification.setVerified(false);
        verification.setClaim(claim);
        verification.setUser(claimant);

        Verification savedVerification = verificationRepository.save(verification);

        // In a real app, you would send an email here
        System.out.println("Verification code for claim " + claimId + ": " + code);
        System.out.println("This would be sent to the item owner in production");

        return mapToDTO(savedVerification);
    }

    public VerificationDTO verifyClaim(String code, Long claimId) {
        Verification verification = verificationRepository.findByCodeAndClaimId(code, claimId)
                .orElseThrow(() -> new RuntimeException("Invalid verification code"));

        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        Verification updatedVerification = verificationRepository.save(verification);

        return mapToDTO(updatedVerification);
    }

    public VerificationDTO getVerificationByClaimId(Long claimId) {
        Verification verification = verificationRepository.findByClaimId(claimId)
                .orElseThrow(() -> new RuntimeException("Verification not found"));
        return mapToDTO(verification);
    }

    private VerificationDTO mapToDTO(Verification verification) {
        return VerificationDTO.builder()
                .id(verification.getId())
                .isVerified(verification.isVerified())
                .code(verification.getCode())
                .verifiedAt(verification.getVerifiedAt())
                .userId(verification.getUser().getId())
                .claimId(verification.getClaim().getId())
                .build();
    }
}
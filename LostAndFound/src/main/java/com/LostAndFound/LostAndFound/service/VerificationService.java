package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.VerificationDTO;
import com.LostAndFound.LostAndFound.model.Verification;
import com.LostAndFound.LostAndFound.repository.ClaimRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import com.LostAndFound.LostAndFound.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;

    public VerificationDTO createVerification(VerificationDTO dto) {
        Verification verification = new Verification();
        verification.setCode(dto.getCode());
        verification.setVerifiedAt(LocalDateTime.now());

        verification.setUser(
                userRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );

        if (dto.getClaimId() != null) {
            verification.setClaim(
                    claimRepository.findById(dto.getClaimId())
                            .orElseThrow(() -> new RuntimeException("Claim not found"))
            );
        }

        return toDTO(verificationRepository.save(verification));
    }

    public VerificationDTO getVerificationById(Long id) {
        return verificationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Verification not found"));
    }

    public void deleteVerification(Long id) {
        verificationRepository.deleteById(id);
    }

    private VerificationDTO toDTO(Verification verification) {
        VerificationDTO dto = new VerificationDTO();
        dto.setId(verification.getId());
        dto.setCode(verification.getCode());
        dto.setUserId(verification.getUser() != null ? verification.getUser().getId() : null);
        dto.setClaimId(verification.getClaim() != null ? verification.getClaim().getId() : null);
        return dto;
    }
}

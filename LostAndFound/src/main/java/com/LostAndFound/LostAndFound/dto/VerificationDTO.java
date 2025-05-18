package com.LostAndFound.LostAndFound.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDTO {
    private Long id;
    private boolean isVerified;
    private String code;
    private LocalDateTime verifiedAt;
    private Long userId;
    private Long claimId;
}
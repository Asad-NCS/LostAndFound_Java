package com.LostAndFound.LostAndFound.dto;

import lombok.Data;

@Data
public class VerificationDTO {
    private Long id;
    private String code;
    private Long userId;
    private Long claimId;
}
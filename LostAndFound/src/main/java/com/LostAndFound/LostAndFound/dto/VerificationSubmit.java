package com.LostAndFound.LostAndFound.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSubmit {
    private String code;
    private Long claimId;
}
package com.LostAndFound.LostAndFound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimDTO {
    private Long id;

    @NotBlank(message = "Description is required")
    private String description;

    private boolean isApproved;
    private Long userId;
    private Long itemId;
}
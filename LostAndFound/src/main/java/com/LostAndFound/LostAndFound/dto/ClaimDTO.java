package com.LostAndFound.LostAndFound.dto;

import com.LostAndFound.LostAndFound.model.ClaimStatus; // Import the new enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDTO {
    private Long id;

    @NotBlank(message = "Claim description/proof is required.")
    private String description;

    private String proofImagePath; // Path to the uploaded proof image (read-only for client in most cases)

    private ClaimStatus status; // Use the enum

    private LocalDateTime claimDate;

    @NotNull(message = "User ID of the claimant is required.")
    private Long userId; // ID of the user making the claim

    private String username; // Username of the claimant (for display)

    @NotNull(message = "Item ID being claimed is required.")
    private Long itemId; // ID of the item being claimed

    // Remove 'approved' boolean if using ClaimStatus
    // private boolean approved;
}
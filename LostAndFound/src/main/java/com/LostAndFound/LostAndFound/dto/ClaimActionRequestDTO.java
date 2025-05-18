package com.LostAndFound.LostAndFound.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor; // Added for completeness, though NoArgsConstructor and Data are key

@Data // This Lombok annotation generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor // Useful if you ever need to construct it with all args
public class ClaimActionRequestDTO {
    private Long userId; // ID of the user performing the action (approver/rejecter/finder)
    private String rejectionReason; // Optional, only for reject action
}

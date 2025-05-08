package com.LostAndFound.LostAndFound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportDTO {
    private Long id;

    @NotBlank(message = "Reason is required")
    private String reason;

    private Long reporterId;
    private Long postId;
    private Long adminId;
}
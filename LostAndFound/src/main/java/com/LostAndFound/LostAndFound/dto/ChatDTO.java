package com.LostAndFound.LostAndFound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatDTO {
    private Long id;

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private LocalDateTime timestamp;
    private Long senderId;
    private Long receiverId;

    // Unified constructor
    public ChatDTO(Long id, String message, LocalDateTime timestamp, Long senderId, Long receiverId) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }
}
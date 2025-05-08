package com.LostAndFound.LostAndFound.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private Long id;
    private String message;
    private boolean isRead;
    private Long userId;

    public NotificationDTO(Long id, String message, boolean isRead, Long aLong) {
        this.id = id;
        this.message = message;
        this.isRead = isRead;
    }

}
package com.LostAndFound.LostAndFound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostDTO {
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    private String title;

    private String description;

    private Long userId;
    private Long itemId;

    public PostDTO(Long id, String title, String description, Long userId, Long itemId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.itemId = itemId;
    }

    public PostDTO() {

    }
}

package com.LostAndFound.LostAndFound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private boolean isLost;
    private String location;

    // For file uploads
    private transient MultipartFile image;

    // For responses
    private String imageUrl;

    private Long userId;
    private Long categoryId;

    public ItemDTO(Long id, String title, String description, boolean isLost, String location, String imageUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isLost = isLost;
        this.location = location;
        this.imageUrl = imageUrl;
    }
}
package com.LostAndFound.LostAndFound.dto;

import com.LostAndFound.LostAndFound.model.Item;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @JsonProperty("isLost")
    private boolean isLost;

    @NotBlank(message = "Location is required")
    private String location;

    private boolean claimed = false;

    @NotBlank(message = "Category is required")
    private String category;

    private String imageUrl;  // Will be populated from getImageUrl()
    private String imagePath; // Direct file path
    private Long userId;
    private Long categoryId;

    public ItemDTO() {}

    @JsonCreator
    public ItemDTO(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("isLost") boolean isLost,
            @JsonProperty("location") String location,
            @JsonProperty("category") String category) {
        this.title = title;
        this.description = description;
        this.isLost = isLost;
        this.location = location;
        this.category = category;
    }

    public ItemDTO(Item item) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.description = item.getDescription();
        this.isLost = item.isLost();
        this.claimed = item.isClaimed();
        this.location = item.getLocation();
        this.imagePath = item.getImagePath();
        this.imageUrl = item.getImageUrl(); // Uses the helper method from Item
        this.userId = item.getUser() != null ? item.getUser().getId() : null;

        if (item.getCategory() != null) {
            this.category = item.getCategory().getName();
            this.categoryId = item.getCategory().getId();
        }
    }
}
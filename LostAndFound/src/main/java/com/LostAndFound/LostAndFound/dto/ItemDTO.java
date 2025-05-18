package com.LostAndFound.LostAndFound.dto;

import com.LostAndFound.LostAndFound.model.Item;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @JsonProperty("isLost")
    private boolean isLost;

    @NotBlank(message = "Location is required")
    private String location;

    private boolean claimed = false;

    @NotBlank(message = "Category is required")
    private String category; // Name of the category

    private String imageUrl;

    // Nested user object for reporter details
    private UserNestedDTO user; // User who reported this item

    private Long categoryId;

    // Fields for claimed item details
    private UserNestedDTO claimedByUser; // User who successfully claimed it
    private LocalDateTime claimedDate;

    public ItemDTO() {}

    // Constructor for creating/updating an item (used by frontend when sending data)
    // This constructor might not need all fields like imageUrl, claimedByUser, etc.
    // It's primarily for data coming *from* the client.
    @JsonCreator
    public ItemDTO(
            @JsonProperty("id") Long id,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("isLost") boolean isLost,
            @JsonProperty("location") String location,
            @JsonProperty("category") String category,
            @JsonProperty("userId") Long userId // Client sends reporter's userId
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isLost = isLost;
        this.location = location;
        this.category = category;
        // When creating, the client sends userId, but ItemService will populate the nested user object.
        // So, this constructor doesn't directly create UserNestedDTO from just a userId.
        // The full UserNestedDTO is populated when converting from Entity to DTO.
        if (userId != null) {
            // Temporary placeholder if needed, but service should build the full UserNestedDTO
            this.user = new UserNestedDTO(userId, null);
        }
    }

    // Constructor to map from Item Entity (used when sending data TO frontend)
    public ItemDTO(Item item) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.description = item.getDescription();
        this.isLost = item.isLost();
        this.claimed = item.isClaimed();
        this.location = item.getLocation();
        this.imageUrl = item.getImageUrl(); // Uses the helper method from Item model

        if (item.getUser() != null) {
            // Populate the nested user object
            this.user = new UserNestedDTO(item.getUser().getId(), item.getUser().getUsername());
        }

        if (item.getCategory() != null) {
            this.category = item.getCategory().getName();
            this.categoryId = item.getCategory().getId();
        }

        if (item.getClaimedByUser() != null) {
            this.claimedByUser = new UserNestedDTO(item.getClaimedByUser().getId(), item.getClaimedByUser().getUsername());
        }
        this.claimedDate = item.getClaimedDate();
    }
}
    
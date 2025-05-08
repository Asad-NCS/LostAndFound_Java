package com.LostAndFound.LostAndFound.dto; // Fixed package

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "Category name is required")
    private String name;

    // Added constructor for service convenience
    public CategoryDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Default constructor for JSON parsing
    public CategoryDTO() {}
}
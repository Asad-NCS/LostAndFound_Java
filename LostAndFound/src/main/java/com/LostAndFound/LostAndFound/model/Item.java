package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.*; // Ensure you have Getter, Setter, NoArgsConstructor, AllArgsConstructor, Builder
import java.time.LocalDateTime; // Import for claimedDate

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(name = "is_lost", nullable = false)
    private boolean isLost; // true if reported as "LOST" by owner, false if reported as "FOUND" by finder

    @Column(name = "is_claimed", nullable = false)
    private boolean claimed = false; // True if an approved claim exists and item is considered returned/resolved

    @Column(name = "image_path")
    private String imagePath; // Image of the item itself (the unique filename, e.g., uuid.jpg)

    @ManyToOne // The user who reported this item (Owner if isLost=true, Finder if isLost=false)
    @JoinColumn(name = "user_id", nullable = false) // Item should always have a reporter
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // New fields for tracking successful claim
    @ManyToOne // User who successfully claimed this item (if any)
    @JoinColumn(name = "claimed_by_user_id")
    private User claimedByUser;

    @Column(name = "claimed_date")
    private LocalDateTime claimedDate;


    // Helper method to construct the publicly accessible URL for the item's image
    public String getImageUrl() {
        if (this.imagePath == null || this.imagePath.isBlank()) {
            return null; // No image path, so no URL to return
        }
        // This constructs the path that the frontend will use to fetch the image.
        // It corresponds to the @GetMapping("/api/items/images/{filename:.+}") endpoint in ItemController.
        return "/api/items/images/" + this.imagePath;
    }
}
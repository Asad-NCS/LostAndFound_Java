package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private boolean isLost;
    private String imagePath;
    private String location; // ← ADD THIS

    @ManyToOne
    private User user;

    @ManyToOne
    private Category category;
}


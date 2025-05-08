package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // One category can be used in many posts
    @OneToMany(mappedBy = "category")
    private List<Post> posts;

}


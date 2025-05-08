package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reason;

    private LocalDateTime reportedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // who reported

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin; // who reviewed it


}

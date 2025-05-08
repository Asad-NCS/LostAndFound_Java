package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Setter
@Entity
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isVerified;

    private String code; // <-- ADD THIS

    private LocalDateTime verifiedAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "claim_id") // <-- OPTIONAL
    private Claim claim;

    public Verification(Long id, boolean isVerified, String code, LocalDateTime verifiedAt, User user, Claim claim) {
        this.id = id;
        this.isVerified = isVerified;
        this.code = code;
        this.verifiedAt = verifiedAt;
        this.user = user;
        this.claim = claim;
    }
}

package com.LostAndFound.LostAndFound.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String password;

    @OneToMany(mappedBy = "user")
    private List<Post> posts;

    @OneToMany(mappedBy = "user")
    private List<Item> items;

    @OneToMany(mappedBy = "user")
    private List<Claim> claims;

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "sender")
    private List<Chat> sentMessages;

    @OneToMany(mappedBy = "receiver")
    private List<Chat> receivedMessages;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Verification verification;

    public User(Long id, String username, String email,
                String password, List<Post> posts, List<Item> items,
                List<Claim> claims, List<Notification> notifications,
                List<Chat> sentMessages, List<Chat> receivedMessages, Verification verification) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.posts = posts;
        this.items = items;
        this.claims = claims;
        this.notifications = notifications;
        this.sentMessages = sentMessages;
        this.receivedMessages = receivedMessages;
        this.verification = verification;
    }
}

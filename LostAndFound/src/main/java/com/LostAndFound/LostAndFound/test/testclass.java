package com.LostAndFound.LostAndFound.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class testclass {
    public static void main(String[] args) {
        String rawPassword = "newPassword123";  // use any password you want
        String hashed = new BCryptPasswordEncoder().encode(rawPassword);
        System.out.println("Hashed Password: " + hashed);
    }
}


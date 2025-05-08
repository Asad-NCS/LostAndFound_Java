package com.LostAndFound.LostAndFound.test;

import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    // Authenticate with plain text (not recommended for production!)
    public boolean authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            System.out.println("User not found for email: " + email);
            return false;
        }

        System.out.println("Raw password entered: " + rawPassword);
        System.out.println("Password from DB: " + user.getPassword());

        if (rawPassword.equals(user.getPassword())) {
            System.out.println("Password matches!");
            return true;
        } else {
            System.out.println("Password does not match!");
            return false;
        }
    }
}

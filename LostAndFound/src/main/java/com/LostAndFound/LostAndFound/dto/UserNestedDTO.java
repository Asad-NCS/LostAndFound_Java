package com.LostAndFound.LostAndFound.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNestedDTO {
    private Long id;
    private String username;
    // Add email if needed by frontend directly within item.user.email
    // private String email;
}
    
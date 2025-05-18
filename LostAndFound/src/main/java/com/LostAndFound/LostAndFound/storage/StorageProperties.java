package com.LostAndFound.LostAndFound.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component; // Or @Configuration

@Getter
@Setter
@Component // Or @Configuration - makes it a Spring bean and eligible for @ConfigurationProperties
@ConfigurationProperties("storage")
public class StorageProperties {

    /**
     * Folder location for storing files.
     */
    private String location = "uploads"; // Default upload directory relative to project root

    // Lombok will generate getters and setters
}
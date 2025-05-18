package com.LostAndFound.LostAndFound.config;

import com.LostAndFound.LostAndFound.storage.FileStorageService;
import com.LostAndFound.LostAndFound.storage.FileSystemStorageService;
import com.LostAndFound.LostAndFound.storage.StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

    // The StorageProperties bean (annotated with @Component and @ConfigurationProperties)
    // will be automatically created by Spring.
    // We can then inject it here if FileSystemStorageService's constructor requires it.

    @Bean
    public FileStorageService fileStorageService(StorageProperties properties) {
        // Pass the properties to the constructor of your actual service implementation
        return new FileSystemStorageService(properties);
    }
}
package com.LostAndFound.LostAndFound.config;

import com.LostAndFound.LostAndFound.storage.FileStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class FileStorageConfig {

    @Bean
    public FileStorageService fileStorageService() {
        return new FileStorageService() {
            @Override
            public String store(MultipartFile file) {
                return "";
            }

            @Override
            public Resource load(String filename) {
                return null;
            }

            @Override
            public void delete(String filename) {

            }

            @Override
            public Resource loadFileAsResource(String imagePath) {
                return null;
            }
        };
    }
}


package com.LostAndFound.LostAndFound.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file); // Returns stored filename
    Resource load(String filename);   // For downloading files
    void delete(String filename);    // For cleanup

    Resource loadFileAsResource(String imagePath);
}
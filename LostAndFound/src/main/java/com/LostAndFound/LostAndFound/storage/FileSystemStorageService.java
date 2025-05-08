package com.LostAndFound.LostAndFound.storage;

import com.LostAndFound.LostAndFound.storage.exception.StorageException;
import com.LostAndFound.LostAndFound.storage.exception.StorageFileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
abstract class FileSystemStorageService implements FileStorageService {

    private final Path rootLocation;


    @Autowired
    public FileSystemStorageService(StorageProperties properties) throws IOException {
        this.rootLocation = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        Files.createDirectories(rootLocation); // Auto-creates uploads folder
    }
    @Override
    public String store(MultipartFile file) {
        try {
            // Generate unique filename
            String filename = UUID.randomUUID() + "_" +
                    StringUtils.cleanPath(file.getOriginalFilename());

            // Save file to the target location
            Path targetLocation = this.rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return filename; // e.g. "3a4b5c6d-lostphone.jpg"
        } catch (IOException ex) {
            throw new StorageException("Failed to store file: " + file.getOriginalFilename(), ex);
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (Exception ex) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, ex);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new StorageException("Failed to delete file: " + filename, ex);
        }
    }

}
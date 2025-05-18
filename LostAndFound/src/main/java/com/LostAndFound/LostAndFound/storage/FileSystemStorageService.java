package com.LostAndFound.LostAndFound.storage;

import com.LostAndFound.LostAndFound.storage.exception.StorageException;
import com.LostAndFound.LostAndFound.storage.exception.StorageFileNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileSystemStorageService implements FileStorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        if (properties.getLocation() == null || properties.getLocation().isBlank()) {
            throw new StorageException("File upload location cannot be empty in StorageProperties.");
        }
        // Ensure rootLocation is absolute and normalized from the start
        this.rootLocation = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        System.out.println("FileSystemStorageService: Configured rootLocation to absolute path: " + this.rootLocation);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation); // rootLocation is already absolute
            System.out.println("FileSystemStorageService: Successfully initialized storage directory: " + this.rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location: " + this.rootLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new StorageException("Original filename contains invalid path sequence '..': " + originalFilename);
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

        try (InputStream inputStream = file.getInputStream()) {
            // Since rootLocation is already absolute & normalized, resolving the unique filename should be safe.
            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize();

            // Security check: ensure the parent of the destination is indeed the rootLocation.
            // This check is more robust now that rootLocation is guaranteed to be absolute.
            if (!destinationFile.getParent().equals(this.rootLocation)) {
                System.err.println("!!! CRITICAL SECURITY CHECK FAILED !!!");
                System.err.println("Destination Parent: " + destinationFile.getParent());
                System.err.println("Configured Root Location: " + this.rootLocation);
                System.err.println("Attempted Filename (unique): " + uniqueFilename);
                System.err.println("Original Filename: " + originalFilename);
                throw new StorageException("Cannot store file outside configured root directory. Attempted path: " + destinationFile);
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("FileSystemStorageService: Stored file: " + destinationFile);
            return uniqueFilename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file '" + originalFilename + "' as '" + uniqueFilename + "'", e);
        }
    }

    // Helper to get Path object, not part of interface but useful internally
    public Path getPath(String filename) {
        return rootLocation.resolve(filename).normalize(); // rootLocation is already absolute
    }

    @Override
    public Resource load(String filename) {
        return loadFileAsResource(filename);
    }

    @Override
    public Resource loadFileAsResource(String imagePath) {
        try {
            Path file = getPath(imagePath);
            Resource resource = new UrlResource(file.toUri());
            System.out.println("FileSystemStorageService: Attempting to load resource from path: " + file);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                System.out.println("FileSystemStorageService: Could not read or find file: " + imagePath + " at path: " + file);
                throw new StorageFileNotFoundException("Could not read file: " + imagePath);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file (Malformed URL): " + imagePath, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = getPath(filename);
            Files.deleteIfExists(file);
            System.out.println("FileSystemStorageService: Deleted file: " + file);
        } catch (IOException e) {
            // You might want to log this as a warning instead of throwing an exception if deleting a non-existent file is acceptable.
            System.err.println("FileSystemStorageService: Failed to delete file '" + filename + "'. Error: " + e.getMessage());
            throw new StorageException("Failed to delete file: " + filename, e);
        }
    }
}
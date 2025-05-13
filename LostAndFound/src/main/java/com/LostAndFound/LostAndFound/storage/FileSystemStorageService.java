package com.LostAndFound.LostAndFound.storage;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileSystemStorageService {
    private final Path rootLocation = Paths.get("uploads");

    public String store(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        Path destinationFile = rootLocation.resolve(Paths.get(filename))
                .normalize()
                .toAbsolutePath();

        if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new SecurityException("Cannot store file outside current directory");
        }

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public Resource loadFileAsResource(String filename) throws MalformedURLException {
        Path filePath = rootLocation.resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read file: " + filename);
        }
    }
}
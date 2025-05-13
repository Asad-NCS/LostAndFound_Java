package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.ItemDTO;
import com.LostAndFound.LostAndFound.model.Category;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.repository.CategoryRepository;
import com.LostAndFound.LostAndFound.service.ItemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final String uploadDirectory = "uploads";

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createItem(
            @RequestPart("item") String itemJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ItemDTO itemDTO = objectMapper.readValue(itemJson, ItemDTO.class);

            if (itemDTO.getCategory() == null || itemDTO.getCategory().isBlank()) {
                throw new IllegalArgumentException("Category is required");
            }

            ItemDTO createdItem = itemService.createItem(itemDTO, image);
            return ResponseEntity.ok(createdItem);
        } catch (Exception e) {
            log.error("Error creating item", e);
            return ResponseEntity.internalServerError()
                    .body("Error creating item: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getItemImage(@PathVariable Long id) {
        try {
            Resource image = itemService.loadItemImage(id);
            String contentType = determineContentType(image.getFilename());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(image);
        } catch (Exception e) {
            log.error("Error loading image for item {}", id, e);
            Resource defaultImage = itemService.loadDefaultImage();
            String contentType = determineContentType(defaultImage.getFilename());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(defaultImage);
        }
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Resource image = itemService.loadImage(filename);
            String contentType = determineContentType(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(image);
        } catch (Exception e) {
            log.error("Error loading image {}", filename, e);
            Resource defaultImage = itemService.loadDefaultImage();
            String contentType = determineContentType(defaultImage.getFilename());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(defaultImage);
        }
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claimItem(@PathVariable Long id) {
        try {
            itemService.claimItem(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error claiming item {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        try {
            return ResponseEntity.ok(itemService.getAllItems());
        } catch (Exception e) {
            log.error("Error getting all items", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestPart("item") String itemJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ItemDTO itemDTO = objectMapper.readValue(itemJson, ItemDTO.class);
            ItemDTO updatedItem = itemService.updateItem(id, itemDTO, image);
            return ResponseEntity.ok(updatedItem);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid JSON format");
        } catch (Exception e) {
            log.error("Error updating item", e);
            return ResponseEntity.internalServerError().body("Error updating item");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        try {
            itemService.deleteItem(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting item", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String filename) {
        if (filename == null) return "image/jpeg";

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) return "image/png";
        if (lowerFilename.endsWith(".gif")) return "image/gif";
        if (lowerFilename.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void validateImageFile(MultipartFile image) {
        if (!image.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (image.getSize() > 5_000_000) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
    }

    private String storeImage(MultipartFile image) throws IOException {
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = System.currentTimeMillis() + extension;

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return newFilename;
    }

    private Long getCategoryIdByName(String categoryName) {
        String trimmedName = categoryName.trim();
        return categoryRepository.findByNameIgnoreCase(trimmedName)
                .orElseThrow(() -> {
                    List<String> validCategories = categoryRepository.findAll()
                            .stream()
                            .map(Category::getName)
                            .collect(Collectors.toList());
                    return new IllegalArgumentException(
                            "Invalid category '" + trimmedName + "'. Valid categories are: " +
                                    String.join(", ", validCategories)
                    );
                })
                .getId();
    }
}
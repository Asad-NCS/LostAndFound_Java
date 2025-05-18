package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.ItemDTO;
// Category model import might not be needed here if not used directly
// import com.LostAndFound.LostAndFound.model.Category;
import com.LostAndFound.LostAndFound.repository.CategoryRepository; // Keep if getCategoryIdByName is used
import com.LostAndFound.LostAndFound.service.ItemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus; // For HttpStatus
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
// Unused imports can be removed
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map; // For error responses
// import java.util.stream.Collectors; // Keep if getCategoryIdByName is used

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    // private final CategoryRepository categoryRepository; // Only if getCategoryIdByName is actively used
    private final ObjectMapper objectMapper;
    // private final String uploadDirectory = "uploads"; // This is handled by FileStorageService now

    // Inside ItemController.java

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createItem(
            @RequestPart("item") String itemJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            log.info("Received create item request. Item JSON: {}", itemJson);
            ItemDTO itemDTO = objectMapper.readValue(itemJson, ItemDTO.class);

            if (itemDTO.getCategory() == null || itemDTO.getCategory().isBlank()) {
                log.warn("Create item failed: Category is required.");
                return ResponseEntity.badRequest().body(Map.of("error", "Category is required"));
            }

            // CORRECTED CHECK FOR USER ID:
            // The ItemDTO now has a nested UserNestedDTO for the 'user' field.
            // The userId for the reporter is sent by the client within the JSON that maps to ItemDTO's
            // constructor or fields. The ItemService then uses this to fetch and set the full User entity.
            // For validation here, we check if the user object or its ID within the DTO is present.
            // The client (React Native) sends `userId` in the JSON payload for `itemDataStr`.
            // The `ItemDTO` constructor `public ItemDTO(@JsonProperty("userId") Long userId ...)`
            // initializes `this.user = new UserNestedDTO(userId, null);` if userId is provided.
            if (itemDTO.getUser() == null || itemDTO.getUser().getId() == null) {
                log.warn("Create item failed: User ID for the reporter is required within the item data.");
                return ResponseEntity.badRequest().body(Map.of("error", "User ID for the reporter must be provided."));
            }
            // The ItemService will use itemDTO.getUser().getId() to fetch the User entity.

            ItemDTO createdItem = itemService.createItem(itemDTO, image);
            log.info("Successfully created item ID: {}", createdItem.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
        } catch (IllegalArgumentException e) {
            log.error("Error creating item due to illegal argument: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        catch (JsonProcessingException e) {
            log.error("Error processing item JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid item data format."));
        }
        catch (Exception e) {
            log.error("Unexpected error creating item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while creating the item: " + e.getMessage()));
        }
    }

    // *** NEW ENDPOINT TO GET ITEM BY ID ***
    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById(@PathVariable Long id) {
        try {
            log.info("Request to get item by ID: {}", id);
            ItemDTO itemDTO = itemService.getItemById(id);
            return ResponseEntity.ok(itemDTO);
        } catch (RuntimeException e) { // Catches "Item not found" from service
            log.warn("Item not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching item ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getItemImage(@PathVariable Long id) {
        try {
            log.debug("Request for image of item ID: {}", id);
            Resource image = itemService.loadItemImage(id); // This already handles default if specific not found
            if (image == null) { // Should be handled by loadItemImage returning default, but as a safeguard
                log.warn("Image resource is null for item ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            String contentType = determineContentType(image.getFilename());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(image);
        } catch (Exception e) { // Catch broader exceptions from service layer
            log.error("Error loading image for item ID {}: {}", id, e.getMessage());
            // Attempt to load a generic default image if any error occurs
            try {
                Resource defaultImage = itemService.loadDefaultImage();
                String contentType = determineContentType(defaultImage.getFilename());
                return ResponseEntity.status(HttpStatus.OK) // Still OK, but serving default
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(defaultImage);
            } catch (Exception defaultEx) {
                log.error("Critical error: Default image also failed to load: {}", defaultEx.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            log.debug("Request for image by filename: {}", filename);
            Resource image = itemService.loadImage(filename); // This handles default if specific not found
            if (image == null) {
                log.warn("Image resource is null for filename: {}", filename);
                return ResponseEntity.notFound().build();
            }
            String contentType = determineContentType(filename); // Use original filename for content type
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(image);
        } catch (Exception e) {
            log.error("Error loading image by filename {}: {}", filename, e.getMessage());
            try {
                Resource defaultImage = itemService.loadDefaultImage();
                String contentType = determineContentType(defaultImage.getFilename());
                return ResponseEntity.status(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(defaultImage);
            } catch (Exception defaultEx) {
                log.error("Critical error: Default image also failed to load: {}", defaultEx.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    // This endpoint directly claims an item without the detailed Claim entity process.
    // Consider if this is still needed or if all claims should go through ClaimController.
    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claimItem(@PathVariable Long id) {
        try {
            // TODO: This needs to know WHICH user is claiming. Should take userId in body or from Auth.
            // itemService.claimItem(id, claimingUserId);
            itemService.claimItem(id); // Original method only takes itemId
            log.info("Item ID {} directly claimed via /api/items/{id}/claim", id);
            return ResponseEntity.ok(Map.of("message", "Item marked as claimed successfully."));
        } catch (IllegalStateException e) {
            log.warn("Error directly claiming item ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error claiming item ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "search", required = false) String searchTerm
            // TODO: Add pagination parameters
    ) {
        try {
            log.info("Request for all items. Filter: {}, Search: {}", filter, searchTerm);
            // TODO: Update ItemService.getAllItems to accept filter and searchTerm
            List<ItemDTO> items = itemService.getAllItems(); // Currently fetches all
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Error getting all items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Or body(Map.of("error", "Could not fetch items"))
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestPart("item") String itemJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            log.info("Received update item request for ID: {}. Item JSON: {}", id, itemJson);
            ItemDTO itemDTO = objectMapper.readValue(itemJson, ItemDTO.class);
            // TODO: Add authorization - only item owner or admin should update
            ItemDTO updatedItem = itemService.updateItem(id, itemDTO, image);
            log.info("Successfully updated item ID: {}", updatedItem.getId());
            return ResponseEntity.ok(updatedItem);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.error("Error updating item {} due to bad request data: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) { // Catches "Item not found" from service
            log.error("Error updating item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        catch (Exception e) {
            log.error("Unexpected error updating item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while updating the item: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            // TODO: Add authorization - only item owner or admin should delete
            log.info("Request to delete item ID: {}", id);
            itemService.deleteItem(id);
            log.info("Successfully deleted item ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) { // Catches "Item not found"
            log.warn("Failed to delete item ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        catch (Exception e) {
            log.error("Error deleting item ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
        }
    }

    private String determineContentType(String filename) {
        if (filename == null || filename.isBlank()) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lowerFilename.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (lowerFilename.endsWith(".webp")) return "image/webp"; // MediaType doesn't have WEBP constant
        // Default to JPEG for .jpg and .jpeg
        if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        return MediaType.APPLICATION_OCTET_STREAM_VALUE; // Fallback
    }

    // The following methods seem like internal logic that might belong more in the service or are unused
    // private void validateImageFile(MultipartFile image) { ... } // This logic is in ItemService
    // private String storeImage(MultipartFile image) throws IOException { ... } // This logic is in FileStorageService & ItemService
    // private Long getCategoryIdByName(String categoryName) { ... } // This logic is in ItemService (findByNameIgnoreCase)
}
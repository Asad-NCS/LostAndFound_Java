package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ItemDTO;
import com.LostAndFound.LostAndFound.model.Category;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.CategoryRepository;
import com.LostAndFound.LostAndFound.repository.ItemRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import com.LostAndFound.LostAndFound.storage.FileStorageService;
import com.LostAndFound.LostAndFound.storage.exception.StorageFileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Objects; // Make sure this is imported for Objects.requireNonNull
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private static final String DEFAULT_IMAGE_FILENAME = "Image_not_available.png";

    public Resource loadItemImage(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
        log.info("Attempting to load image for item ID: {}. Stored imagePath: {}", id, item.getImagePath());
        if (item.getImagePath() == null || item.getImagePath().isBlank()) {
            log.info("Image path is null or empty for item ID: {}. Loading default image.", id);
            return loadDefaultImage();
        }
        try {
            Resource resource = fileStorageService.loadFileAsResource(item.getImagePath());
            if (resource != null && resource.exists() && resource.isReadable()) {
                log.info("Successfully loaded image: {} for item ID: {}", item.getImagePath(), id);
                return resource;
            } else {
                log.warn("Image file not found or not readable for item ID: {} (path: {}). Loading default image.", id, item.getImagePath());
                return loadDefaultImage();
            }
        } catch (StorageFileNotFoundException e) {
            log.warn("StorageFileNotFoundException for item ID: {} (path: {}). Loading default image. Error: {}", id, item.getImagePath(), e.getMessage());
            return loadDefaultImage();
        } catch (Exception e) {
            log.error("Generic error loading image for item ID: {} (path: {}). Loading default image. Error: {}", id, item.getImagePath(), e.getMessage(), e);
            return loadDefaultImage();
        }
    }

    public Resource loadImage(String filename) {
        log.info("Attempting to load image directly by filename: {}", filename);
        try {
            Resource resource = fileStorageService.loadFileAsResource(filename);
            if (resource != null && resource.exists() && resource.isReadable()) {
                log.info("Successfully loaded image: {}", filename);
                return resource;
            } else {
                log.warn("Direct image load: file not found or not readable (filename: {}). Loading default image.", filename);
                return loadDefaultImage();
            }
        } catch (StorageFileNotFoundException e) {
            log.warn("Direct image load: StorageFileNotFoundException (filename: {}). Loading default image. Error: {}", filename, e.getMessage());
            return loadDefaultImage();
        } catch (Exception e) {
            log.error("Direct image load: Generic error (filename: {}). Loading default image. Error: {}", filename, e.getMessage(), e);
            return loadDefaultImage();
        }
    }

    public Resource loadDefaultImage() {
        log.info("Attempting to load default image: {}", DEFAULT_IMAGE_FILENAME);
        try {
            Resource resource = fileStorageService.loadFileAsResource(DEFAULT_IMAGE_FILENAME);
            if (resource.exists() && resource.isReadable()) {
                log.info("Default image loaded successfully.");
                return resource;
            } else {
                log.error("Default image '{}' not found or not readable in storage. Ensure it exists in the configured uploads directory.", DEFAULT_IMAGE_FILENAME);
                throw new RuntimeException("Critical: Default image not found or unreadable.");
            }
        } catch (Exception e) {
            log.error("Failed to load default image '{}'. Error: {}", DEFAULT_IMAGE_FILENAME, e.getMessage(), e);
            throw new RuntimeException("Failed to load default image due to: " + e.getMessage(), e);
        }
    }

    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemDTO createItem(ItemDTO dto, MultipartFile image) { // Line 119 is around here
        validateItemDTO(dto);
        User user;

        // CORRECTED ACCESS TO USER ID FROM DTO
        if (dto.getUser() != null && dto.getUser().getId() != null) {
            user = userRepository.findById(dto.getUser().getId()) // Use dto.getUser().getId()
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + dto.getUser().getId()));
        } else {
            throw new IllegalArgumentException("User ID is required to create an item.");
        }

        Category category = categoryRepository.findByNameIgnoreCase(dto.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category: " + dto.getCategory()));

        Item item = new Item();
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setLost(dto.isLost());
        item.setClaimed(false);
        item.setLocation(dto.getLocation());
        item.setCategory(category);
        item.setUser(user); // Assign the fetched User entity

        if (image != null && !image.isEmpty()) {
            validateImageFile(image);
            try {
                String storedFilename = fileStorageService.store(image);
                item.setImagePath(storedFilename);
                log.info("Associated image '{}' with new item '{}'", storedFilename, item.getTitle());
            } catch (Exception e) {
                log.error("Failed to store image for new item '{}'. Error: {}", item.getTitle(), e.getMessage(), e);
                throw new RuntimeException("Could not store image for the item. Please try again.", e);
            }
        }

        Item savedItem = itemRepository.save(item);
        return convertToDto(savedItem);
    }

    @Transactional
    public ItemDTO updateItem(Long id, ItemDTO dto, MultipartFile image) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
        validateItemDTO(dto);

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setLost(dto.isLost());
        item.setLocation(dto.getLocation());

        if (!item.getCategory().getName().equalsIgnoreCase(dto.getCategory())) {
            Category category = categoryRepository.findByNameIgnoreCase(dto.getCategory())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category: " + dto.getCategory()));
            item.setCategory(category);
        }

        // If you need to update the user associated with an item (less common for updates)
        // Ensure dto.getUser() and dto.getUser().getId() are not null before comparing
        if (dto.getUser() != null && dto.getUser().getId() != null &&
                (item.getUser() == null || !dto.getUser().getId().equals(item.getUser().getId()))) {
            User newUser = userRepository.findById(dto.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found for update with ID: " + dto.getUser().getId()));
            item.setUser(newUser);
        }


        if (image != null && !image.isEmpty()) {
            validateImageFile(image);
            if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
                try {
                    fileStorageService.delete(item.getImagePath());
                    log.info("Deleted old image '{}' for updated item ID: {}", item.getImagePath(), id);
                } catch (Exception e) {
                    log.warn("Could not delete old image '{}' for item ID: {}. Error: {}", item.getImagePath(), id, e.getMessage());
                }
            }
            try {
                String storedFilename = fileStorageService.store(image);
                item.setImagePath(storedFilename);
                log.info("Updated image to '{}' for item ID: {}", storedFilename, id);
            } catch (Exception e) {
                log.error("Failed to store image for updated item ID: {}. Error: {}", id, e.getMessage(), e);
                throw new RuntimeException("Could not store image for the item update. Please try again.", e);
            }
        }

        Item updatedItem = itemRepository.save(item);
        return convertToDto(updatedItem);
    }

    public ItemDTO getItemById(Long id) {
        return itemRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
    }

    @Transactional
    public void claimItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemId));
        if (item.isClaimed()) {
            throw new IllegalStateException("Item with ID: " + itemId + " is already claimed.");
        }
        if (!item.isLost()){
            throw new IllegalStateException("Item with ID: " + itemId + " was reported as 'Found', not 'Lost'. Use the detailed claim process.");
        }
        item.setClaimed(true);
        itemRepository.save(item);
        log.info("Item with ID: {} directly marked as claimed via ItemService.", itemId);
    }

    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
        if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            try {
                fileStorageService.delete(item.getImagePath());
                log.info("Deleted image '{}' for deleted item ID: {}", item.getImagePath(), id);
            } catch (Exception e) {
                log.warn("Could not delete image '{}' for item ID: {}. It might have already been deleted or was not found. Error: {}",
                        item.getImagePath(), id, e.getMessage());
            }
        }
        itemRepository.deleteById(id);
        log.info("Deleted item with ID: {}", id);
    }

    private void validateItemDTO(ItemDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Item title is required.");
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("Item category is required.");
        }
        if (dto.getLocation() == null || dto.getLocation().isBlank()) {
            throw new IllegalArgumentException("Item location is required.");
        }
    }

    private void validateImageFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        // Use Objects.requireNonNull to ensure contentType is not null before calling startsWith
        if (!Objects.requireNonNull(image.getContentType(), "Image content type cannot be null").startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed (e.g., PNG, JPG, GIF). Received: " + image.getContentType());
        }
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (image.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB. Actual size: " + (image.getSize() / (1024*1024)) + "MB");
        }
    }

    private ItemDTO convertToDto(Item item) {
        return new ItemDTO(item); // Assumes ItemDTO constructor correctly maps from Item entity
    }
}

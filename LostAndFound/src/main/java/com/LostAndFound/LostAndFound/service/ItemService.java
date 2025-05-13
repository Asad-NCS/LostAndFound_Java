package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ItemDTO;
import com.LostAndFound.LostAndFound.model.Category;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.CategoryRepository;
import com.LostAndFound.LostAndFound.repository.ItemRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import com.LostAndFound.LostAndFound.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final Path rootLocation = Paths.get("uploads");

    public Resource loadItemImage(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        if (item.getImagePath() == null || item.getImagePath().isEmpty()) {
            return loadDefaultImage();
        }

        return loadImage(item.getImagePath());
    }

    public Resource loadImage(String filename) {
        try {
            if (filename == null || filename.isEmpty()) {
                return loadDefaultImage();
            }

            Path filePath = rootLocation.resolve(filename).normalize();
            Resource resource = fileStorageService.loadFileAsResource(filename);

            if (!resource.exists() || !resource.isReadable()) {
                return loadDefaultImage();
            }

            return resource;
        } catch (Exception e) {
            log.error("Failed to load image: {}", filename, e);
            return loadDefaultImage();
        }
    }

    public Resource loadDefaultImage() {
        try {
            return fileStorageService.loadFileAsResource("default.jpg");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load default image");
        }
    }

    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ItemDTO createItem(ItemDTO dto, MultipartFile image) throws IOException {
        validateItemDTO(dto);

        Category category = categoryRepository.findByNameIgnoreCase(dto.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category: " + dto.getCategory()));

        Item item = new Item();
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setLost(dto.isLost());
        item.setClaimed(dto.isClaimed());
        item.setLocation(dto.getLocation());
        item.setCategory(category);

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            item.setUser(user);
        }

        if (image != null && !image.isEmpty()) {
            validateImageFile(image);
            String fileName = storeImageFile(image);
            item.setImagePath(fileName); // Store just the filename
            log.info("Stored image with filename: {}", fileName);
        }

        Item savedItem = itemRepository.save(item);
        return convertToDto(savedItem);
    }

    private String storeImageFile(MultipartFile image) throws IOException {
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = System.currentTimeMillis() + extension;

        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return newFilename; // Return just the filename
    }

    private ItemDTO convertToDto(Item item) {
        ItemDTO dto = new ItemDTO(item); // This will automatically set both imagePath and imageUrl
        return dto;
    }

    public ItemDTO updateItem(Long id, ItemDTO dto, MultipartFile image) throws IOException {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        validateItemDTO(dto);

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setLost(dto.isLost());
        item.setClaimed(dto.isClaimed());
        item.setLocation(dto.getLocation());

        if (!dto.getCategory().equals(item.getCategory().getName())) {
            Category category = categoryRepository.findByNameIgnoreCase(dto.getCategory())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category"));
            item.setCategory(category);
        }

        if (dto.getUserId() != null && !dto.getUserId().equals(item.getUser().getId())) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            item.setUser(user);
        }

        if (image != null && !image.isEmpty()) {
            validateImageFile(image);
            String fileName = fileStorageService.store(image);
            item.setImagePath(fileName);
        }

        return convertToDto(itemRepository.save(item));
    }

    public ItemDTO getItemById(Long id) {
        return convertToDto(itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id)));
    }

    public void claimItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        if (item.isClaimed()) {
            throw new IllegalStateException("Item already claimed");
        }

        item.setClaimed(true);
        itemRepository.save(item);
        log.info("Item claimed: {}", itemId);
    }

    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new RuntimeException("Item not found: " + id);
        }
        itemRepository.deleteById(id);
    }

    private void validateItemDTO(ItemDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title required");
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category required");
        }
        if (dto.getLocation() == null || dto.getLocation().isBlank()) {
            throw new IllegalArgumentException("Location required");
        }
    }

    private void validateImageFile(MultipartFile image) {
        if (!image.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only images allowed");
        }
        if (image.getSize() > 5_000_000) {
            throw new IllegalArgumentException("Max 5MB file size");
        }
    }

}
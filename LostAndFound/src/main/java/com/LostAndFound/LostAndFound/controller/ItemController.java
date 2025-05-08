package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.ItemDTO;
import com.LostAndFound.LostAndFound.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // Create with image upload
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemDTO> createItem(
            @RequestPart("item") ItemDTO itemDTO,
            @RequestPart("image") MultipartFile image) throws IOException {
        ItemDTO createdItem = itemService.createItem(itemDTO, image);
        return ResponseEntity.ok(createdItem);
    }

    // Get item details
    @GetMapping("/{id}")
    public ResponseEntity<ItemDTO> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    // Download item image
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getItemImage(@PathVariable Long id) {
        Resource image = itemService.loadItemImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg") // Adjust based on file type
                .body(image);
    }

    // Get all items
    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    // Update item (with optional new image)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemDTO> updateItem(
            @PathVariable Long id,
            @RequestPart("item") ItemDTO itemDTO,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        return ResponseEntity.ok(itemService.updateItem(id, itemDTO, image));
    }

    // Delete item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
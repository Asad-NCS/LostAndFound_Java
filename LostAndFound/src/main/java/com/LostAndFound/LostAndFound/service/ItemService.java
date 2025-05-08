package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ItemDTO;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.repository.ItemRepository;
import com.LostAndFound.LostAndFound.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final FileStorageService fileStorageService;

    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll().stream().map(item ->
                new ItemDTO(
                        item.getId(),
                        item.getTitle(),
                        item.getDescription(),
                        item.isLost(),
                        item.getLocation(),
                        "/api/images/" + item.getImagePath()
                )
        ).toList();
    }


    public ItemDTO createItem(ItemDTO dto, MultipartFile image) throws IOException {
        Item item = new Item();
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setLost(dto.isLost());
        item.setLocation(dto.getLocation());
        item.setImagePath(fileStorageService.store(image));
        itemRepository.save(item);
        return new ItemDTO(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.isLost(),
                item.getLocation(),
                "/api/images/" + item.getImagePath()
        );
    }
    public ItemDTO updateItem(Long id, ItemDTO dto, MultipartFile image) throws IOException {
        Item item = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setLost(dto.isLost());
        item.setLocation(dto.getLocation());

        if (image != null && !image.isEmpty()) {
            item.setImagePath(fileStorageService.store(image));
        }

        itemRepository.save(item);

        return new ItemDTO(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.isLost(),
                item.getLocation(),
                "/api/images/" + item.getImagePath()
        );
    }

    public ItemDTO getItemById(Long id) {
        Optional<Item> item = itemRepository.findById(id);
        return item.map(i -> new ItemDTO(
                i.getId(),
                i.getTitle(),
                i.getDescription(),
                i.isLost(),
                i.getLocation(),
                "/api/images/" + i.getImagePath()
        )).orElse(null);
    }

    public Resource loadItemImage(Long id) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));
        return fileStorageService.loadFileAsResource(item.getImagePath());
    }

    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }


}
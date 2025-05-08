package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.ChatDTO;
import com.LostAndFound.LostAndFound.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatDTO> createChat(@RequestBody ChatDTO chatDTO) {
        return ResponseEntity.ok(chatService.createChat(chatDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatDTO> getChat(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getChatById(id));
    }

    @GetMapping
    public ResponseEntity<List<ChatDTO>> getAllChats() {
        return ResponseEntity.ok(chatService.getAllChats());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatDTO> updateChat(@PathVariable Long id, @RequestBody ChatDTO chatDTO) {
        return ResponseEntity.ok(chatService.updateChat(id, chatDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id) {
        chatService.deleteChat(id);
        return ResponseEntity.noContent().build();
    }
}

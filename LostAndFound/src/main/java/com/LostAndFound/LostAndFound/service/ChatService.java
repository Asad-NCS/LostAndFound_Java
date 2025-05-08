package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ChatDTO;
import com.LostAndFound.LostAndFound.model.Chat;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.ChatRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public ChatDTO createChat(ChatDTO dto) {
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Chat chat = new Chat();
        chat.setMessage(dto.getMessage());
        chat.setTimestamp(LocalDateTime.now());
        chat.setSender(sender);
        chat.setReceiver(receiver);

        Chat savedChat = chatRepository.save(chat);
        return convertToDTO(savedChat);
    }

    public ChatDTO getChatById(Long id) {
        return chatRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
    }

    public List<ChatDTO> getAllChats() {
        return chatRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ChatDTO updateChat(Long id, ChatDTO dto) {
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chat.setMessage(dto.getMessage());
        Chat updatedChat = chatRepository.save(chat);
        return convertToDTO(updatedChat);
    }

    public void deleteChat(Long id) {
        chatRepository.deleteById(id);
    }

    private ChatDTO convertToDTO(Chat chat) {
        return new ChatDTO(
                chat.getId(),
                chat.getMessage(),
                chat.getTimestamp(),
                chat.getSender() != null ? chat.getSender().getId() : null,
                chat.getReceiver() != null ? chat.getReceiver().getId() : null
        );
    }
 

}
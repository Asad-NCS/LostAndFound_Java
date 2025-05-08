package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.NotificationDTO;
import com.LostAndFound.LostAndFound.model.Notification;
import com.LostAndFound.LostAndFound.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationDTO createNotification(NotificationDTO dto) {
        Notification notification = new Notification();
        notification.setMessage(dto.getMessage());
        notification.setRead(dto.isRead());
        notificationRepository.save(notification);
        return new NotificationDTO(notification.getId(), notification.getMessage(), notification.isRead(), notification.getUser() != null ? notification.getUser().getId() : null);
    }

    public NotificationDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow();
        notification.setRead(true);
        notificationRepository.save(notification);
        return new NotificationDTO(notification.getId(), notification.getMessage(), true, notification.getUser() != null ? notification.getUser().getId() : null);
    }

    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(notification -> new NotificationDTO(
                        notification.getId(),
                        notification.getMessage(),
                        notification.isRead(),
                        notification.getUser() != null ? notification.getUser().getId() : null
                ))
                .toList();
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
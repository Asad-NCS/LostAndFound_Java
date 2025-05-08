package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    @Query("SELECT c FROM Chat c WHERE " +
            "(c.sender.id = :senderId AND c.receiver.id = :receiverId) OR " +
            "(c.sender.id = :receiverId AND c.receiver.id = :senderId) " +
            "ORDER BY c.timestamp ASC")
    List<Chat> findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId);
}
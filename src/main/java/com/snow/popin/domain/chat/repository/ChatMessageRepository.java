package com.snow.popin.domain.chat.repository;

import com.snow.popin.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByReservationIdOrderBySentAtAsc(Long reservationId);
}

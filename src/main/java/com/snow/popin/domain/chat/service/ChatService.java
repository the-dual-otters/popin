package com.snow.popin.domain.chat.service;

import com.snow.popin.domain.chat.entity.ChatMessage;
import com.snow.popin.domain.chat.repository.ChatMessageRepository;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.repository.SpaceReservationRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SpaceReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessage saveMessage(Long reservationId, String content, Long senderId) {
        log.info("[ChatService] 메시지 저장 요청: reservationId={}, senderId={}, content={}",
                reservationId, senderId, content);

        SpaceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 예약을 찾을 수 없음: reservationId={}", reservationId);
                    return new IllegalArgumentException("예약을 찾을 수 없습니다.");
                });

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 사용자를 찾을 수 없음: senderId={}", senderId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        if (reservation.getStatus() == ReservationStatus.REJECTED ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.warn("[ChatService] 채팅 불가 상태: reservationId={}, status={}",
                    reservationId, reservation.getStatus());
            throw new IllegalStateException("이 예약 상태에서는 채팅할 수 없습니다.");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .reservation(reservation)
                .sender(sender)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);
        log.info("[ChatService] 메시지 저장 완료: id={}, reservationId={}, senderId={}",
                saved.getId(), reservationId, senderId);

        return saved;
    }

    public List<ChatMessage> getMessages(Long reservationId) {
        log.info("[ChatService] 메시지 목록 조회: reservationId={}", reservationId);
        List<ChatMessage> messages = chatMessageRepository.findByReservationIdOrderBySentAtAsc(reservationId);
        log.info("[ChatService] 메시지 조회 완료: reservationId={}, count={}", reservationId, messages.size());
        return messages;
    }
}

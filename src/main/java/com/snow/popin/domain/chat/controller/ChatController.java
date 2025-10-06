package com.snow.popin.domain.chat.controller;

import com.snow.popin.domain.chat.dto.ChatMessageDto;
import com.snow.popin.domain.chat.entity.ChatMessage;
import com.snow.popin.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageDto dto) {
        log.info("[ChatController] 채팅 메시지 수신: reservationId={}, senderId={}, content={}",
                dto.getReservationId(), dto.getSenderId(), dto.getContent());

        try {
            ChatMessage saved = chatService.saveMessage(
                    dto.getReservationId(),
                    dto.getContent(),
                    dto.getSenderId()
            );

            log.info("[ChatController] 채팅 메시지 저장 성공: id={}, reservationId={}",
                    saved.getId(), saved.getReservation().getId());

            ChatMessageDto response = new ChatMessageDto(saved);

            messagingTemplate.convertAndSend(
                    "/topic/reservation/" + dto.getReservationId(),
                    response
            );

            log.info("[ChatController] 채팅 메시지 브로드캐스트 완료: reservationId={}", dto.getReservationId());

        } catch (Exception e) {
            log.error("[ChatController] 채팅 메시지 처리 실패: reservationId={}, senderId={}, error={}",
                    dto.getReservationId(), dto.getSenderId(), e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "메시지 전송 실패: " + e.getMessage());

            messagingTemplate.convertAndSend(
                    "/topic/reservation/" + dto.getReservationId(),
                    errorResponse
            );
        }
    }
}

package com.snow.popin.domain.chat.dto;

import com.snow.popin.domain.chat.entity.ChatMessage;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long reservationId;
    private Long senderId;
    private String senderName;
    private String content;
    private String sentAt;

    public ChatMessageDto(ChatMessage message) {
        this.reservationId = message.getReservation().getId();
        this.senderId = message.getSender().getId();

        // 닉네임 우선순위 처리
        String nickname = message.getSender().getNickname();
        String name = message.getSender().getName();
        String email = message.getSender().getEmail();

        if (nickname != null && !nickname.trim().isEmpty()) {
            this.senderName = nickname;
        } else if (name != null && !name.trim().isEmpty()) {
            this.senderName = name;
        } else if (email != null && !email.trim().isEmpty()) {
            String[] emailParts = email.split("@");
            this.senderName = emailParts.length > 0 ? emailParts[0] : "익명";
        } else {
            this.senderName = "익명";
        }

        this.content = message.getContent();
        this.sentAt = message.getSentAt().toString();
    }
}
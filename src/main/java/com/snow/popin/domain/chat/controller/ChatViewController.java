package com.snow.popin.domain.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ChatViewController {

    @GetMapping("/chat/{reservationId}")
    public String chatPage(@PathVariable Long reservationId) {
        return "forward:/templates/pages/chat/chat.html";
    }
}
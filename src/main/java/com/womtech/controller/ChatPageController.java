package com.womtech.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

import com.womtech.entity.Chat;
import com.womtech.repository.ChatRepository;

@Controller
public class ChatPageController {

    private final ChatRepository chatRepository;

    public ChatPageController(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    // Trang chat cho USER
    @GetMapping("/user/chat")
    public String userChatPage(@RequestParam String chatId, Principal principal, Model model) {
        String currentUserId = principal != null ? principal.getName() : null;
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("chatId", chatId);
        return "user/chat";
    }

    // Trang chat cho VENDOR (support)
    @GetMapping("/vendor/chat")
    public String vendorChatPage(
            @RequestParam(required = false) String chatId, 
            Principal principal, Model model) {
        if (principal == null) return "redirect:/auth/login";

        model.addAttribute("currentVendorId", principal.getName());
        model.addAttribute("chatId", chatId); // null nếu chưa chọn chat
        return "vendor/chat";
    }
}
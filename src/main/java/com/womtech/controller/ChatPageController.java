package com.womtech.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

	// Trang khung chat cho user
	@GetMapping("/chat")
	public String chatPage(@RequestParam(required = false) String chatID, Model model) {
		model.addAttribute("chatID", chatID); // có thể null -> UI tự tạo chat mới
		return "user/chat"; // templates/user/chat.html
	}

	// (tuỳ chọn) Trang cho agent/support xem danh sách chat
	@GetMapping("/support/chats")
	public String supportChatsPage() {
		return "support/chats"; // templates/support/chats.html
	}
}

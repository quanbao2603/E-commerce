package com.womtech.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ChatPageController {

	// Trang chat cho USER
	@GetMapping("/user/chat")
	public String userChatPage(@RequestParam String chatId, Principal principal, Model model) {
		String currentUserId = principal != null ? principal.getName() : null;
		model.addAttribute("currentUserId", currentUserId);
		model.addAttribute("chatId", chatId);
		return "chat/user-chat"; // templates/chat/user-chat.html
	}

	// Trang chat cho VENDOR (support)
	@GetMapping("/vendor/chat")
	public String vendorChatPage(@RequestParam String chatId, Principal principal, Model model) {
		String currentVendorId = principal != null ? principal.getName() : null;
		model.addAttribute("currentVendorId", currentVendorId);
		model.addAttribute("chatId", chatId);
		return "chat/vendor-chat"; // templates/chat/vendor-chat.html
	}
}

package com.womtech.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

	
	@GetMapping("/chat")
	public String chatPage(@RequestParam(required = false) String chatID, Model model) {
		model.addAttribute("chatID", chatID); 
		return "user/chat"; 
	}

	
	@GetMapping("/support/chats")
	public String supportChatsPage() {
		return "support/chats"; 
	}
}

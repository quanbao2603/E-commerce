package com.womtech.api;

import com.womtech.dto.request.chat.CreateChatRequest;
import com.womtech.dto.request.chat.SendMessageRequest;
import com.womtech.dto.response.chat.ChatMessageResponse;
import com.womtech.dto.response.chat.ChatSummaryResponse;
import com.womtech.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatRestController {

	private final ChatService chatService;

	// Tạo (hoặc lấy) luồng chat cho user hiện tại (supportID có thể null)
	@PostMapping
	public ChatSummaryResponse createOrGet(Principal principal, @RequestBody(required = false) CreateChatRequest req) {
		String userID = principal.getName(); // bạn đã map Principal -> userID
		return chatService.createOrGetChat(userID, req);
	}

	// Danh sách chat của user hiện tại
	@GetMapping("/me")
	public List<ChatSummaryResponse> myChats(Principal principal) {
		return chatService.listChatsOfUser(principal.getName());
	}

	// Danh sách chat do support hiện tại phụ trách
	@GetMapping("/me/supporting")
	public List<ChatSummaryResponse> myAssigned(Principal principal) {
		return chatService.listChatsOfSupport(principal.getName());
	}

	// Lấy lịch sử tin nhắn (mặc định page=0, size=20)
	@GetMapping("/{chatID}/messages")
	public Page<ChatMessageResponse> messages(@PathVariable String chatID, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return chatService.getMessages(chatID, page, size);
	}

	// Gửi tin nhắn (fallback qua REST, ngoài WebSocket)
	@PostMapping("/{chatID}/messages")
	public ChatMessageResponse send(@PathVariable String chatID, @Valid @RequestBody SendMessageRequest body,
			Principal principal) {
		return chatService.sendMessage(chatID, principal.getName(), body);
	}
}

package com.womtech.ws;

import com.womtech.dto.request.chat.SendMessageRequest;
import com.womtech.dto.response.chat.ChatMessageResponse;
import com.womtech.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

	private final ChatService chatService;
	private final SimpMessageSendingOperations messagingTemplate;

	public static class WsSendPayload {
		public String chatID;
		public String message;
	}

	@MessageMapping("/chat.send")
	public void send(@Payload WsSendPayload payload, Principal principal) {
		SendMessageRequest req = new SendMessageRequest();
		req.setMessage(payload.message);

		ChatMessageResponse saved = chatService.sendMessage(payload.chatID, principal.getName(), req);

		messagingTemplate.convertAndSend("/topic/chat." + payload.chatID, saved);
	}
}
package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Chat;
import com.womtech.service.ChatService;

@Service
public class ChatServiceImpl extends BaseServiceImpl<Chat, String> implements ChatService {
	public ChatServiceImpl(JpaRepository<Chat, String> repo) {
		super(repo);
	}
}
package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
	
}
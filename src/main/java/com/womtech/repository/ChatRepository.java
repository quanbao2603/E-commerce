package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Chat;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
	
}
package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Post;
import com.womtech.service.PostService;

@Service
public class PostServiceImpl extends BaseServiceImpl<Post, String> implements PostService {
	public PostServiceImpl(JpaRepository<Post, String> repo) {
		super(repo);
	}
}
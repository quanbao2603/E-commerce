package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Review;
import com.womtech.service.ReviewService;

@Service
public class ReviewServiceImpl extends BaseServiceImpl<Review, String> implements ReviewService {
	protected ReviewServiceImpl(JpaRepository<Review, String> repo) {
		super(repo);
	}
}
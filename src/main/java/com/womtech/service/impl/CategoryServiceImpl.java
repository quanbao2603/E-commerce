package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Category;
import com.womtech.service.CategoryService;

@Service
public class CategoryServiceImpl extends BaseServiceImpl<Category, String> implements CategoryService {
	public CategoryServiceImpl(JpaRepository<Category, String> repo) {
		super(repo);
	}
}
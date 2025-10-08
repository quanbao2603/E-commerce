package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.womtech.entity.Subcategory;
import com.womtech.service.SubcategoryService;

@Service
public class SubcategoryServiceImpl extends BaseServiceImpl<Subcategory, String> implements SubcategoryService {
	public SubcategoryServiceImpl(JpaRepository<Subcategory, String> repo) {
		super(repo);
	}
}

package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Brand;
import com.womtech.service.BrandService;

@Service
public class BrandServiceImpl extends BaseServiceImpl<Brand, String> implements BrandService {
	public BrandServiceImpl(JpaRepository<Brand, String> repo) {
		super(repo);
	}
}
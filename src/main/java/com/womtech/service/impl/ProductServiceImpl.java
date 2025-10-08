package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Product;
import com.womtech.service.ProductService;

@Service
public class ProductServiceImpl extends BaseServiceImpl<Product, String> implements ProductService {
	public ProductServiceImpl(JpaRepository<Product, String> repo) {
		super(repo);
	}
}
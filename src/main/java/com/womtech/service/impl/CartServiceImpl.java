package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Cart;
import com.womtech.service.CartService;

@Service
public class CartServiceImpl extends BaseServiceImpl<Cart, String> implements CartService {
	public CartServiceImpl(JpaRepository<Cart, String> repo) {
		super(repo);
	}
}
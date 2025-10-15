package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.womtech.entity.CartItem;
import com.womtech.service.CartItemService;

@Service
public class CartItemServiceImpl extends BaseServiceImpl<CartItem, String> implements CartItemService {
	public CartItemServiceImpl(JpaRepository<CartItem, String> repo) {
		super(repo);
	}
}
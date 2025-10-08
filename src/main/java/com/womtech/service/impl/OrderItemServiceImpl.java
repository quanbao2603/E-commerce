package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.OrderItem;
import com.womtech.service.OrderItemService;

@Service
public class OrderItemServiceImpl extends BaseServiceImpl<OrderItem, String> implements OrderItemService {
	public OrderItemServiceImpl(JpaRepository<OrderItem, String> repo) {
		super(repo);
	}
}
package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Order;
import com.womtech.service.OrderService;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, String> implements OrderService {
	public OrderServiceImpl(JpaRepository<Order, String> repo) {
		super(repo);
	}
}
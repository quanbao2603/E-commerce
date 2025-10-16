package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.OrderItem;
import com.womtech.repository.OrderItemRepository;
import com.womtech.service.OrderItemService;

@Service
public class OrderItemServiceImpl extends BaseServiceImpl<OrderItem, String> implements OrderItemService {
	private final OrderItemRepository orderItemRepository;

	public OrderItemServiceImpl(JpaRepository<OrderItem, String> repo) {
		super(repo);
		this.orderItemRepository = (OrderItemRepository) repo;
	}

	@Override
	public boolean hasUserPurchasedProduct(String userId, String productId) {
		return orderItemRepository.hasUserPurchasedProduct(userId, productId);
	}
}
package com.womtech.service;

import com.womtech.entity.OrderItem;

public interface OrderItemService extends BaseService<OrderItem, String> {
	boolean hasUserPurchasedProduct(String userId, String productId);
}
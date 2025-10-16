package com.womtech.service;

import java.util.List;

import com.womtech.entity.Address;
import com.womtech.entity.Order;
import com.womtech.entity.User;

public interface OrderService extends BaseService<Order, String> {

	List<Order> findByUser(User user);

	Order createOrder(User user, Address address, String payment_method);

}
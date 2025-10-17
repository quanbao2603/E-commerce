package com.womtech.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.womtech.entity.Address;
import com.womtech.entity.Cart;
import com.womtech.entity.Order;
import com.womtech.entity.User;
import com.womtech.repository.OrderRepository;
import com.womtech.service.AddressService;
import com.womtech.service.CartItemService;
import com.womtech.service.CartService;
import com.womtech.service.OrderItemService;
import com.womtech.service.OrderService;
import com.womtech.util.OrderStatusHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, String> implements OrderService {
	@Autowired
	CartService cartService;
	@Autowired
    CartItemService cartItemService;
	@Autowired
    AddressService addressService;
	@Autowired
	OrderItemService orderItemService;
	@Autowired
    OrderRepository orderRepository;

	public OrderServiceImpl(JpaRepository<Order, String> repo) {
		super(repo);
	}

    @Override
	public Order createOrder(User user, Address address, String payment_method) {
        Cart cart = cartService.findByUser(user);
        // Chưa thêm voucher
        BigDecimal total = cartService.totalPrice(cart);
        
        Order order = Order.builder()
                .user(user)
                .address(address)
                .totalPrice(total)
                .paymentMethod(payment_method)
                .totalPrice(total)
                .createAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build();

        orderItemService.createItemsFromCart(order, cart);
        
        orderRepository.save(order);
        
        cartService.clearCart(user);

        return order;
    }

	@Override
	public List<Order> findByUser(User user) {
		return orderRepository.findByUser(user);
	}
}

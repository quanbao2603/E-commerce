package com.womtech.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.womtech.entity.OrderVoucher;
import com.womtech.entity.OrderVoucherID;
import com.womtech.repository.OrderVoucherRepository;
import com.womtech.service.OrderVoucherService;

@Service
public class OrderVoucherServiceImpl extends BaseServiceImpl<OrderVoucher, OrderVoucherID> implements OrderVoucherService {
	@Autowired
	OrderVoucherRepository orderVoucherRepository;
	
	public OrderVoucherServiceImpl(OrderVoucherRepository repo) {
		super(repo);
	}
}

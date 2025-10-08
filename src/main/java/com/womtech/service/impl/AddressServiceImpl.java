package com.womtech.service.impl;

import com.womtech.entity.Address;
import com.womtech.repository.AddressRepository;
import com.womtech.service.AddressService;
import org.springframework.stereotype.Service;

@Service
public class AddressServiceImpl extends BaseServiceImpl<Address, String> implements AddressService {
	public AddressServiceImpl(AddressRepository repo) {
		super(repo);
	}
}
package com.womtech.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.womtech.entity.Address;
import com.womtech.entity.User;
import com.womtech.repository.AddressRepository;
import com.womtech.service.AddressService;

@Service
public class AddressServiceImpl implements AddressService {
	@Autowired
	AddressRepository addressRepository;

	@Override
	public <S extends Address> S save(S entity) {
		return addressRepository.save(entity);
	}

	@Override
	public Optional<Address> findById(String id) {
		return addressRepository.findById(id);
	}

	@Override
	public void deleteById(String id) {
		addressRepository.deleteById(id);
	}

	@Override
	public Optional<Address> findByUserAndIsDefaultTrue(User user) {
		return addressRepository.findByUserAndIsDefaultTrue(user);
	}

	@Override
	public List<Address> findByIsDefaultTrue() {
		return addressRepository.findByIsDefaultTrue();
	}
}

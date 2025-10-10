package com.womtech.service;

import java.util.List;
import java.util.Optional;

import com.womtech.entity.Address;
import com.womtech.entity.User;

public interface AddressService extends BaseService<Address, String> {

	Optional<Address> findByUserAndIsDefaultTrue(User user);

	List<Address> findByUser(User user);
	
	 void unsetDefaultForUser(User user);
}
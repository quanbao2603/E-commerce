package com.womtech.service;

import java.util.List;
import java.util.Optional;

import com.womtech.entity.Address;
import com.womtech.entity.User;

public interface AddressService {

	void deleteById(String id);

	Optional<Address> findById(String id);

	<S extends Address> S save(S entity);

	List<Address> findByIsDefaultTrue();

	Optional<Address> findByUserAndIsDefaultTrue(User user);

}

package com.womtech.service;

import java.util.Optional;

import com.womtech.dto.request.RegisterRequest;
import com.womtech.dto.response.RegisterResponse;
import com.womtech.entity.User;

public interface UserService {

	void deleteById(String id);

	Optional<User> findById(String id);

	<S extends User> S save(S entity);

	Optional<User> findByEmail(String email);
	
	RegisterResponse register(RegisterRequest request);

}

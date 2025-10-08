package com.womtech.service;

import java.util.Optional;

import com.womtech.dto.request.auth.LoginRequest;
import com.womtech.dto.request.auth.RegisterRequest;
import com.womtech.dto.response.auth.LoginResponse;
import com.womtech.dto.response.auth.RegisterResponse;
import com.womtech.entity.User;

public interface UserService extends BaseService<User, String>{
	Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}	
package com.womtech.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.womtech.dto.request.RegisterRequest;
import com.womtech.dto.response.RegisterResponse;
import com.womtech.entity.User;
import com.womtech.repository.UserRepository;
import com.womtech.service.UserService;
import com.womtech.util.PasswordUtil;
@Service
public class UserServiceImpl implements UserService {
	@Autowired
	UserRepository userRepository;

	@Override
	public <S extends User> S save(S entity) {
		return userRepository.save(entity);
	}

	@Override
	public Optional<User> findById(String id) {
		return userRepository.findById(id);
	}

	@Override
	public void deleteById(String id) {
		userRepository.deleteById(id);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public RegisterResponse register(RegisterRequest request) {  // <-- dùng 'request'
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(PasswordUtil.encode(request.getPassword().trim()))
                .email(request.getEmail().trim())
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .userID(saved.getUserID())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .message("Registered successfully")
                .build();
    }
	}


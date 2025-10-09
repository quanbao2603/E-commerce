package com.womtech.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.dto.request.auth.LoginRequest;
import com.womtech.dto.request.auth.RegisterRequest;
import com.womtech.dto.response.auth.LoginResponse;
import com.womtech.dto.response.auth.RegisterResponse;
import com.womtech.entity.Role;
import com.womtech.entity.User;
import com.womtech.repository.RoleRepository;
import com.womtech.repository.UserRepository;
import com.womtech.service.UserService;
import com.womtech.util.PasswordUtil;

@Service
public class UserServiceImpl extends BaseServiceImpl<User, String> implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	protected UserServiceImpl(JpaRepository<User, String> repo) {
		super(repo);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	@Override
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	@Override
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public RegisterResponse register(RegisterRequest request) {
		if (existsByUsername(request.getUsername())) {
			return RegisterResponse.builder().message("Username already exists").build();
		}
		if (existsByEmail(request.getEmail())) {
			return RegisterResponse.builder().message("Email already registered").build();
		}

		Role defaultRole = roleRepository.findByRolename("USER")
				.orElseThrow(() -> new IllegalStateException("Default role USER not found"));

		String hashedPassword = PasswordUtil.encode(request.getPassword());

		User user = User.builder().username(request.getUsername()).email(request.getEmail()).password(hashedPassword)
				.role(defaultRole).status(1).build();

		userRepository.save(user);

		return RegisterResponse.builder().userID(user.getUserID()).username(user.getUsername()).email(user.getEmail())
				.message("Register successful!").build();
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
		if (userOpt.isEmpty()) {
			return LoginResponse.builder().message("User not found").build();
		}

		User user = userOpt.get();

		if (user.getStatus() == 0) {
			return LoginResponse.builder().message("Account is locked").build();
		}

		if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
			return LoginResponse.builder().message("Invalid password").build();
		}

		return LoginResponse.builder().userID(user.getUserID()).username(user.getUsername())
				.message("Login successful!").redirectUrl("/").build();
	}
}

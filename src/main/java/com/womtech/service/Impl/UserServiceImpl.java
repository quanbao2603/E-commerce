package com.womtech.service.Impl;

import com.womtech.dto.request.RegisterRequest;
import com.womtech.dto.response.RegisterResponse;
import com.womtech.entity.Role;
import com.womtech.entity.User;
import com.womtech.repository.RoleRepository;
import com.womtech.repository.UserRepository;
import com.womtech.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor 
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private static final String DEFAULT_ROLE_NAME = "USER";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Role defaultRole = roleRepo.findByRolenameIgnoreCase(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException("Default role not found: " + DEFAULT_ROLE_NAME));

        String hashedPassword = passwordEncoder.encode(req.getPassword());

        User newUser = User.builder()
                .userID(null)            
                .username(req.getUsername().trim())
                .password(hashedPassword)
                .email(req.getEmail().trim())
                .role(defaultRole)          
                .build();

        User savedUser = userRepo.save(newUser);
      
        return RegisterResponse.builder()
                .userID(savedUser.getUserID())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .message("Registered successfully")
                .build();
    }
}

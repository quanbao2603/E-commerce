package com.womtech.service;

import com.womtech.dto.request.RegisterRequest;
import com.womtech.dto.response.RegisterResponse;

public interface UserService {
    RegisterResponse register(RegisterRequest request);
}

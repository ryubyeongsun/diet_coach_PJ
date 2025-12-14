package com.dietcoach.project.service.auth;

import com.dietcoach.project.dto.auth.AuthResponse;
import com.dietcoach.project.dto.auth.LoginRequest;
import com.dietcoach.project.dto.auth.MeResponse;
import com.dietcoach.project.dto.auth.SignupRequest;

public interface AuthService {
    Long signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    MeResponse me(Long userId);
}

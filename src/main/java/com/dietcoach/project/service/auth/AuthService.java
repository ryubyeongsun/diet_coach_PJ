package com.dietcoach.project.service.auth;

import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.dto.auth.AuthResponse;
import com.dietcoach.project.dto.auth.LoginRequest;
import com.dietcoach.project.dto.auth.SignupRequest;

public interface AuthService {
    Long signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse me(Long userId);
}

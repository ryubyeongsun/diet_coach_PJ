package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.dto.auth.AuthResponse;
import com.dietcoach.project.dto.auth.LoginRequest;
import com.dietcoach.project.dto.auth.SignupRequest;
import com.dietcoach.project.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<Long> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(request);
        return ApiResponse.success("signup success", userId);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(authService.me(userId));
    }
}

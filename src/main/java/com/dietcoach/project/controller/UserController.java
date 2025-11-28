package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for user registration and profile view.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Registers a new user with profile details.
     */
    @PostMapping
    public ApiResponse<Long> register(@RequestBody @Valid UserCreateRequest request) {
        Long userId = userService.registerUser(request);
        return ApiResponse.ok("User registered", userId);
    }

    /**
     * Returns user profile including BMR/TDEE/target calorie.
     */
    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable Long id) {
        UserProfileResponse profile = userService.getUserProfile(id);
        if (profile == null) {
            return ApiResponse.error("User not found");
        }
        return ApiResponse.ok(profile);
    }
}

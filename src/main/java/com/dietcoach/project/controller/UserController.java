package com.dietcoach.project.controller;

import com.dietcoach.project.dto.UserProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.TdeeResponse;
import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원 + 프로필 + TDEE 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createUser(@RequestBody UserCreateRequest request) {
        Long userId = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("회원 생성 완료", userId));
    }

    /**
     * 프로필 + TDEE 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long id) {
        UserProfileResponse profile = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 프로필 수정
     */
    @PutMapping("/{id}/profile")
    public ApiResponse<UserProfileResponse> updateUserProfile(@PathVariable Long id, @RequestBody @Valid UserProfileUpdateRequest request) {
        UserProfileResponse updatedProfile = userService.updateUserProfile(id, request);
        return ApiResponse.success("프로필 업데이트 완료", updatedProfile);
    }

    @GetMapping("/{id}/tdee")
    public ResponseEntity<ApiResponse<TdeeResponse>> getUserTdee(@PathVariable Long id) {
        TdeeResponse tdee = userService.getUserTdee(id);
        return ResponseEntity.ok(ApiResponse.success(tdee));
    }
}

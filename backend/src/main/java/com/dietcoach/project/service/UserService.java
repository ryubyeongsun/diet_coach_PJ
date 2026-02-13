package com.dietcoach.project.service;

import com.dietcoach.project.dto.TdeeResponse;
import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.dto.UserProfileUpdateRequest;

public interface UserService {

    Long createUser(UserCreateRequest request);

    UserProfileResponse getUserProfile(Long userId);

    UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);

    TdeeResponse getUserTdee(Long userId);
}

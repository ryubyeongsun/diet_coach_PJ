package com.dietcoach.project.service;

import com.dietcoach.project.dto.TdeeResponse;
import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;

public interface UserService {

    Long createUser(UserCreateRequest request);

    UserProfileResponse getUserProfile(Long userId);
    TdeeResponse getUserTdee(Long userId);
}

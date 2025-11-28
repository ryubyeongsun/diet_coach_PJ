package com.dietcoach.project.service;

import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;

/**
 * Business operations related to user and profile management.
 */
public interface UserService {

    /**
     * Registers a new user with profile information.
     *
     * @return persisted user id
     */
    Long registerUser(UserCreateRequest request);

    /**
     * Loads user profile with derived metrics such as BMR/TDEE.
     */
    UserProfileResponse getUserProfile(Long userId);
}

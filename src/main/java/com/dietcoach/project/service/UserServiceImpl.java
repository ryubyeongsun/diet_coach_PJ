package com.dietcoach.project.service;

import com.dietcoach.project.domain.User;
import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of UserService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional
    public Long registerUser(UserCreateRequest request) {
        // TODO: 로그인 기능 추가 시 중복 이메일 체크 및 패스워드 해시 적용 필요.
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .gender(request.getGender())
                .age(request.getAge())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .activityLevel(request.getActivityLevel())
                .goalType(request.getGoalType())
                .targetWeightKg(request.getTargetWeightKg())
                .build();

        userMapper.insertUser(user);
        return user.getId();
    }

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return null; // 이후 GlobalExceptionHandler 도입 시 예외로 교체 예정.
        }

        double bmr = TdeeCalculator.calculateBmr(
                user.getGender(),
                user.getWeightKg(),
                user.getHeightCm(),
                user.getAge()
        );

        double tdee = TdeeCalculator.calculateTdee(bmr, user.getActivityLevel());
        double target = TdeeCalculator.calculateTargetCalorie(tdee, user.getGoalType());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .activityLevel(user.getActivityLevel())
                .goalType(user.getGoalType())
                .targetWeightKg(user.getTargetWeightKg())
                .bmr(round(bmr))
                .tdee(round(tdee))
                .targetCalorie(round(target))
                .build();
    }

    private double round(double value) {
        return Math.round(value);
    }
}

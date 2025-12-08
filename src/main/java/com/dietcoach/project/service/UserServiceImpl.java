package com.dietcoach.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.common.TdeeCalculator;
import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.dto.TdeeResponse;
import com.dietcoach.project.dto.UserCreateRequest;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Long createUser(UserCreateRequest request) {

        // 1. 이메일 중복 체크
        if (userMapper.findByEmail(request.getEmail()) != null) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }

        // 2. DTO -> 엔티티 매핑 + 빌더 사용
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // 실제 서비스면 암호화 필요
                .name(request.getName())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .height(request.getHeight())
                .weight(request.getWeight())
                .activityLevel(request.getActivityLevel())
                .goalType(request.getGoalType())
                .build();

        // 3. TDEE 계산
        TdeeCalculator.TdeeResult result = TdeeCalculator.calculate(
                request.getGender(),
                request.getBirthDate(),
                request.getHeight(),
                request.getWeight(),
                request.getActivityLevel(),
                request.getGoalType()
        );

        user.setBmr(result.getBmr());
        user.setTdee(result.getTdee());
        user.setTargetCalories(result.getTargetCalories());

        // 4. DB 저장
        userMapper.insertUser(user);

        return user.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다.");
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .height(user.getHeight())
                .weight(user.getWeight())
                .activityLevel(user.getActivityLevel())
                .goalType(user.getGoalType())
                .bmr(user.getBmr())
                .tdee(user.getTdee())
                .targetCalories(user.getTargetCalories())
                .build();
    }
@Override
@Transactional(readOnly = true)
public TdeeResponse getUserTdee(Long userId) {
    User user = userMapper.findById(userId);
    if (user == null) {
        throw new BusinessException("존재하지 않는 사용자입니다.");
    }

    return TdeeResponse.builder()
            .bmr(user.getBmr() != null ? user.getBmr() : 0.0)
            .tdee(user.getTdee() != null ? user.getTdee() : 0.0)
            .targetCalories(user.getTargetCalories() != null ? user.getTargetCalories() : 0.0)
            .build();
}

}
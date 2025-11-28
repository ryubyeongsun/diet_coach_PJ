package com.dietcoach.project.dto;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;
import lombok.Builder;
import lombok.Data;

/**
 * User profile view including derived metrics such as BMR and TDEE.
 */
@Data
@Builder
public class UserProfileResponse {

    private Long id;

    private String email;

    private String nickname;

    private Gender gender;

    private Integer age;

    private Double heightCm;

    private Double weightKg;

    private ActivityLevel activityLevel;

    private GoalType goalType;

    private Double targetWeightKg;

    // Derived values
    private Double bmr;              // 기초대사량
    private Double tdee;             // 유지칼로리
    private Double targetCalorie;    // 목표 섭취 칼로리
}

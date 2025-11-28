package com.dietcoach.project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User entity including profile information required for TDEE calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;

    private String email;

    // TODO: 초기 개발 단계에서는 plain text일 수 있으나,
    //       실제 서비스에서는 반드시 hashing 적용 필요.
    private String password;

    private String nickname;

    private Gender gender;

    private Integer age;

    private Double heightCm;

    private Double weightKg;

    private ActivityLevel activityLevel;

    private GoalType goalType;

    private Double targetWeightKg;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

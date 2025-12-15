package com.dietcoach.project.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * users 테이블 매핑 엔티티
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;

    private String email;
    private String password;
    private String name;

    private Gender gender;
    private LocalDate birthDate;

    private Double height; // cm
    private Double weight; // kg

    private ActivityLevel activityLevel;
    private GoalType goalType;

    // 계산 결과
    private Double bmr;
    private Double tdee;
    private Double targetCalories;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public boolean isProfileCompleted() {
        return gender != null
                && height != null
                && activityLevel != null
                && goalType != null;
    }
}

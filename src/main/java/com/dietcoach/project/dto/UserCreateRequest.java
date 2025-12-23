package com.dietcoach.project.dto;

import java.time.LocalDate;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    private String email;
    private String password;
    private String name;

    private Gender gender;
    private LocalDate birthDate;

    private Double height;
    private Double weight;
    private Double targetWeight;

    private ActivityLevel activityLevel;
    private GoalType goalType;
}

package com.dietcoach.project.dto;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;
import jakarta.validation.constraints.*;

import lombok.Data;

/**
 * Request payload for initial user registration and profile setup.
 */
@Data
public class UserCreateRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 4, max = 50)
    private String password;

    @NotBlank
    @Size(max = 30)
    private String nickname;

    @NotNull
    private Gender gender;

    @NotNull
    @Min(10)
    @Max(120)
    private Integer age;

    @NotNull
    @Positive
    private Double heightCm;

    @NotNull
    @Positive
    private Double weightKg;

    @NotNull
    private ActivityLevel activityLevel;

    @NotNull
    private GoalType goalType;

    @Positive
    private Double targetWeightKg;
}

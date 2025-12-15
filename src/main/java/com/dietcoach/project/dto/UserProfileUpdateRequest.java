package com.dietcoach.project.dto;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateRequest {

    @NotNull
    private Gender gender;

    @NotNull
    private LocalDate birthDate;

    @NotNull
    private Double height;

    @NotNull
    private Double weight;

    @NotNull
    private ActivityLevel activityLevel;

    @NotNull
    private GoalType goalType;
}

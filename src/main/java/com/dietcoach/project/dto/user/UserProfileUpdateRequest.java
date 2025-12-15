package com.dietcoach.project.dto.user;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateRequest {
    private Gender gender;
    private Double height;
    private ActivityLevel activityLevel;
    private GoalType goalType;
}

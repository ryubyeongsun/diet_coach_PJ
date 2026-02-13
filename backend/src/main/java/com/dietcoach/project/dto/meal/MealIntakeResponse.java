package com.dietcoach.project.dto.meal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealIntakeResponse {
    private Long mealPlanDayId;
    private String mealTime;
    
    @JsonProperty("isConsumed")
    private boolean isConsumed;
    
    private LocalDateTime consumedAt;
}

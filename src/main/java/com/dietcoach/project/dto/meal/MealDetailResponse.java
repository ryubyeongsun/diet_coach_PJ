package com.dietcoach.project.dto.meal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealDetailResponse {
    private String mealTime;
    private int totalCalories;
    
    @JsonProperty("isConsumed")
    private boolean isConsumed;
    
    private List<MealItemResponse> items;
}

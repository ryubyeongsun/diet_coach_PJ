package com.dietcoach.project.dto.shopping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetProposalRequest {
    private Long userId;
    private int monthlyBudget;
    private int targetCalories;
    private int mealsPerDay;
    private List<String> preferences; // e.g., "KOREAN", "HIGH_PROTEIN"
    private List<String> allergies;
}

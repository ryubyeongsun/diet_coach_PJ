package com.dietcoach.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TDEE 정보만 따로 조회할 때 사용하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TdeeResponse {

    private Double bmr;
    private Double tdee;
    private Double targetCalories;
}
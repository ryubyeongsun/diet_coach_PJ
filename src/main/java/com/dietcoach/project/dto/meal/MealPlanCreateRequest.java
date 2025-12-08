package com.dietcoach.project.dto.meal;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MealPlanCreateRequest {

    private Long userId;
    private LocalDate startDate;  // null이면 오늘 기준으로 30일 생성
}

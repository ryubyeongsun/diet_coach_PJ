package com.dietcoach.project.mapper.dashboard.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DayCaloriesRow {
    private LocalDate date;
    private Integer totalCalories;
}

package com.dietcoach.project.mapper.dashboard.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DayWeightRow {
    private LocalDate date;
    private Double weight;
}

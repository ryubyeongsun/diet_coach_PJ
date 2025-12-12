package com.dietcoach.project.dto.weight;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WeightRecordUpsertRequest {
    private LocalDate recordDate;
    private Double weight;
    private String memo;
}

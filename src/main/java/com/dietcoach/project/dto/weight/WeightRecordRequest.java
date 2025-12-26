package com.dietcoach.project.dto.weight;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecordRequest {
    private LocalDate recordDate; // "2025-12-13"
    private Double weight;        // 73.2
    private String memo;          // optional
}

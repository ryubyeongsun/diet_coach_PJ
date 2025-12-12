package com.dietcoach.project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecord {

    private Long id;
    private Long userId;

    private LocalDate recordDate;
    private Double weight;
    private String memo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

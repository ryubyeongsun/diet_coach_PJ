package com.dietcoach.project.dto.weight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BodyStatusResponse {
    private Double height;
    private Double weight;
    private Double bmi;
    private String bmiCategory;
    private Integer characterLevel;
}

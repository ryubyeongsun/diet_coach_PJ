package com.dietcoach.project.dto.weight;

import java.time.LocalDate;

import com.dietcoach.project.domain.WeightRecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecordResponse {
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private Double weight;
    private String memo;

    public static WeightRecordResponse from(WeightRecord r) {
        return WeightRecordResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .recordDate(r.getRecordDate())
                .weight(r.getWeight())
                .memo(r.getMemo())
                .build();
    }
}

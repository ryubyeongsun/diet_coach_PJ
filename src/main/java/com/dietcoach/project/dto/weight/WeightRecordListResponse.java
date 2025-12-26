package com.dietcoach.project.dto.weight;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WeightRecordListResponse {
    private Long userId;
    private String fromDate;
    private String toDate;
    private List<WeightRecordResponse> records;
}

package com.dietcoach.project.service;

import com.dietcoach.project.dto.weight.BodyStatusResponse;
import com.dietcoach.project.dto.weight.WeightRecordResponse;
import com.dietcoach.project.dto.weight.WeightRecordUpsertRequest;

import java.time.LocalDate;
import java.util.List;

public interface WeightRecordService {

    WeightRecordResponse upsertWeightRecord(Long userId, WeightRecordUpsertRequest request);

    List<WeightRecordResponse> getWeightRecords(Long userId, LocalDate from, LocalDate to);

    void deleteWeightRecord(Long userId, Long recordId);

    void deleteWeightRecord(Long userId, LocalDate recordDate);

    BodyStatusResponse getBodyStatus(Long userId);
}

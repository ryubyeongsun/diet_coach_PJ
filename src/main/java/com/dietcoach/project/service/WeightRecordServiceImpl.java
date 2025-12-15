package com.dietcoach.project.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.WeightRecord;
import com.dietcoach.project.dto.weight.WeightRecordListResponse;
import com.dietcoach.project.dto.weight.WeightRecordRequest;
import com.dietcoach.project.dto.weight.WeightRecordResponse;
import com.dietcoach.project.dto.weight.WeightRecordUpsertRequest;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.WeightRecordMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeightRecordServiceImpl implements WeightRecordService {

    private final UserMapper userMapper;
    private final WeightRecordMapper weightRecordMapper;

    @Override
    @Transactional
    public WeightRecordResponse upsertWeightRecord(Long userId, WeightRecordUpsertRequest request) {
        if (userId == null) throw new BusinessException("userId는 필수입니다.");
        if (request == null) throw new BusinessException("요청 바디가 비었습니다.");
        if (request.getRecordDate() == null) throw new BusinessException("recordDate는 필수입니다.");
        if (request.getWeight() == null) throw new BusinessException("weight는 필수입니다.");

        // 1) 유저 존재 확인
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);
        }

        // 2) 같은 날짜 기록 존재 여부 확인
        WeightRecord existing = weightRecordMapper.findByUserIdAndDate(userId, request.getRecordDate());

        if (existing == null) {
            // INSERT
            WeightRecord record = WeightRecord.builder()
                    .userId(userId)
                    .recordDate(request.getRecordDate())
                    .weight(request.getWeight())
                    .memo(request.getMemo())
                    .build();

            weightRecordMapper.insertWeightRecord(record);

            return WeightRecordResponse.builder()
                    .id(record.getId())
                    .userId(record.getUserId())
                    .recordDate(record.getRecordDate())
                    .weight(record.getWeight())
                    .memo(record.getMemo())
                    .build();
        }

        // UPDATE
        existing.setWeight(request.getWeight());
        existing.setMemo(request.getMemo());

        weightRecordMapper.updateWeightRecord(existing);

        return WeightRecordResponse.builder()
                .id(existing.getId())
                .userId(existing.getUserId())
                .recordDate(existing.getRecordDate())
                .weight(existing.getWeight())
                .memo(existing.getMemo())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightRecordResponse> getWeightRecords(Long userId, LocalDate from, LocalDate to) {
        if (userId == null) throw new BusinessException("userId는 필수입니다.");

        // 기본값: 최근 30일
        LocalDate today = LocalDate.now();
        if (to == null) to = today;
        if (from == null) from = to.minusDays(29);

        // 유저 존재 확인
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);
        }

        return weightRecordMapper.findByUserIdBetweenDates(userId, from, to).stream()
                .map(r -> WeightRecordResponse.builder()
                        .id(r.getId())
                        .userId(r.getUserId())
                        .recordDate(r.getRecordDate())
                        .weight(r.getWeight())
                        .memo(r.getMemo())
                        .build())
                .toList();
    }
    @Override
    @Transactional
    public void deleteWeightRecord(Long userId, Long recordId) {
        WeightRecord record = weightRecordMapper.findById(recordId);

        if (record == null) {
            throw new BusinessException("존재하지 않는 기록입니다. id=" + recordId);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("해당 기록을 삭제할 권한이 없습니다.");
        }

        weightRecordMapper.deleteById(recordId);
    }
}
package com.dietcoach.project.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.WeightRecord;
import com.dietcoach.project.dto.weight.BodyStatusResponse;
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

    @Override
    @Transactional
    public void deleteWeightRecord(Long userId, LocalDate recordDate) {
        if (userId == null || recordDate == null) {
            throw new BusinessException("userId와 recordDate는 필수입니다.");
        }
        int deletedCount = weightRecordMapper.deleteByUserIdAndDate(userId, recordDate);
        if (deletedCount == 0) {
            // 이미 없으면 성공으로 간주하거나 에러를 낼 수 있는데, 멱등성을 위해 에러 안 냄?
            // PRD: "해당 날짜 기록 존재 -> 삭제"
            // 없으면 에러를 띄울 수도 있음. 여기선 조용히 넘어감 or 404.
            // 보통 삭제 요청했는데 없으면 404를 주기도 함.
            throw new BusinessException("해당 날짜의 체중 기록이 존재하지 않습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BodyStatusResponse getBodyStatus(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다.");
        }

        Double height = user.getHeight();
        if (height == null || height <= 0) {
            throw new BusinessException("키 정보가 없습니다. 프로필을 설정해주세요.");
        }

        // 최신 체중 조회
        WeightRecord latestRecord = weightRecordMapper.findLatestByUserId(userId);
        Double currentWeight = (latestRecord != null) ? latestRecord.getWeight() : user.getWeight();

        if (currentWeight == null || currentWeight <= 0) {
            // 체중 기록 없음 -> 400 또는 기본 상태(LV3)
            // 여기서는 400 에러 처리하여 사용자에게 입력을 유도
            throw new BusinessException("체중 기록이 없습니다. 체중을 입력해주세요.");
        }

        // BMI 계산
        double heightInMeters = height / 100.0;
        double bmiValue = currentWeight / (heightInMeters * heightInMeters);
        
        // 소수점 둘째 자리 반올림
        double bmi = Math.round(bmiValue * 100.0) / 100.0;

        // BMI Category & Level
        String category;
        int level;

        if (bmi < 18.5) {
            category = "매우 마름";
            level = 1;
        } else if (bmi < 22.0) {
            category = "마름";
            level = 2;
        } else if (bmi < 25.0) {
            category = "정상";
            level = 3;
        } else if (bmi < 30.0) {
            category = "통통";
            level = 4;
        } else {
            category = "비만";
            level = 5;
        }

        return BodyStatusResponse.builder()
                .height(height)
                .weight(currentWeight)
                .bmi(bmi)
                .bmiCategory(category)
                .characterLevel(level)
                .build();
    }
}
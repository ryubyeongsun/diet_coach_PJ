package com.dietcoach.project.service;

import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.WeightRecord;
import com.dietcoach.project.dto.weight.BodyStatusResponse;
import com.dietcoach.project.dto.weight.WeightRecordUpsertRequest;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.WeightRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeightRecordServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private WeightRecordMapper weightRecordMapper;

    @InjectMocks
    private WeightRecordServiceImpl weightRecordService;

    @Test
    @DisplayName("BMI Calculation & Level 4 (Overweight) Check")
    void testGetBodyStatus_Level4() {
        // Given
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setHeight(170.0); // 1.7m
        user.setWeight(75.0);  // Initial weight (if no record exists)

        // Mock: Latest weight record exists
        WeightRecord latestRecord = WeightRecord.builder()
                .userId(userId)
                .weight(72.4) // 72.4 / (1.7 * 1.7) = 25.05 -> Level 4
                .recordDate(LocalDate.now())
                .build();

        when(userMapper.findById(userId)).thenReturn(user);
        when(weightRecordMapper.findLatestByUserId(userId)).thenReturn(latestRecord);

        // When
        BodyStatusResponse response = weightRecordService.getBodyStatus(userId);

        // Then
        assertNotNull(response);
        assertEquals(170.0, response.getHeight());
        assertEquals(72.4, response.getWeight());
        assertEquals(25.05, response.getBmi());
        assertEquals("통통", response.getBmiCategory());
        assertEquals(4, response.getCharacterLevel());
    }

    @Test
    @DisplayName("BMI Calculation & Level 2 (Underweight) Check")
    void testGetBodyStatus_Level2() {
        // Given
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setHeight(180.0); // 1.8m

        // Mock: Latest weight record
        WeightRecord latestRecord = WeightRecord.builder()
                .userId(userId)
                .weight(60.0) // 60 / (1.8 * 1.8) = 18.51 -> Level 2
                .build();

        when(userMapper.findById(userId)).thenReturn(user);
        when(weightRecordMapper.findLatestByUserId(userId)).thenReturn(latestRecord);

        // When
        BodyStatusResponse response = weightRecordService.getBodyStatus(userId);

        // Then
        assertEquals(18.52, response.getBmi()); // 18.518... -> 18.52
        assertEquals("마름", response.getBmiCategory());
        assertEquals(2, response.getCharacterLevel());
    }

    @Test
    @DisplayName("Upsert Weight Record - Update Existing")
    void testUpsertWeight_Update() {
        // Given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2025, 1, 10);
        
        WeightRecordUpsertRequest request = new WeightRecordUpsertRequest();
        request.setRecordDate(date);
        request.setWeight(70.0);
        request.setMemo("Morning");

        User user = new User();
        user.setId(userId);

        WeightRecord existing = WeightRecord.builder()
                .id(100L)
                .userId(userId)
                .recordDate(date)
                .weight(69.0)
                .build();

        when(userMapper.findById(userId)).thenReturn(user);
        when(weightRecordMapper.findByUserIdAndDate(userId, date)).thenReturn(existing);

        // When
        weightRecordService.upsertWeightRecord(userId, request);

        // Then
        verify(weightRecordMapper).updateWeightRecord(any(WeightRecord.class));
        assertEquals(70.0, existing.getWeight()); // Should be updated
    }

    @Test
    @DisplayName("Delete Weight Record by Date")
    void testDeleteWeight_ByDate() {
        // Given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2025, 1, 10);

        when(weightRecordMapper.deleteByUserIdAndDate(userId, date)).thenReturn(1);

        // When
        weightRecordService.deleteWeightRecord(userId, date);

        // Then
        verify(weightRecordMapper).deleteByUserIdAndDate(userId, date);
    }
}

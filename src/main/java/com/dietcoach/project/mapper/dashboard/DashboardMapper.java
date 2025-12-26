package com.dietcoach.project.mapper.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Data;

@Mapper
public interface DashboardMapper {

    List<DayCaloriesRow> findDayCalories(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<DayWeightRow> findWeights(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Data
    class DayCaloriesRow {
        private LocalDate date;
        private Integer totalCalories;
    }

    @Data
    class DayWeightRow {
        private LocalDate date;
        private Double weight;
    }
}

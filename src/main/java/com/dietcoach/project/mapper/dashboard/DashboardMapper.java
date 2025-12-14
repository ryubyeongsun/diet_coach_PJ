package com.dietcoach.project.mapper.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

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

    // === Row DTOs (MyBatis 결과 매핑용) ===
    class DayCaloriesRow {
        private LocalDate date;
        private Integer totalCalories;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public Integer getTotalCalories() { return totalCalories; }
        public void setTotalCalories(Integer totalCalories) { this.totalCalories = totalCalories; }
    }

    class DayWeightRow {
        private LocalDate date;
        private Double weight;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
    }
}

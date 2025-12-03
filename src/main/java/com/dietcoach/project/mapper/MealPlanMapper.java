package com.dietcoach.project.mapper;

import com.dietcoach.project.domain.MealItem;
import com.dietcoach.project.domain.MealPlan;
import com.dietcoach.project.domain.MealPlanDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealPlanMapper {

    int insertMealPlan(MealPlan mealPlan);

    int insertMealPlanDay(MealPlanDay mealPlanDay);

    int insertMealItem(MealItem mealItem);

    MealPlan selectMealPlanById(@Param("mealPlanId") Long mealPlanId);

    MealPlan selectLatestMealPlanByUserId(@Param("userId") Long userId);

    // 상세 조회를 위해 Day/Item도 따로 가져올 수 있게 기본 메서드 정의
    List<MealPlanDay> selectMealPlanDays(@Param("mealPlanId") Long mealPlanId);

    List<MealItem> selectMealItems(@Param("mealPlanDayId") Long mealPlanDayId);
}

package com.dietcoach.project.mapper.meal;

import com.dietcoach.project.domain.meal.MealIntake;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealIntakeMapper {
    // upsert: insert or update
    void upsert(MealIntake mealIntake);

    List<MealIntake> findByDayId(@Param("userId") Long userId, @Param("mealPlanDayId") Long mealPlanDayId);

    // used when regenerating day or replacing meal
    void deleteByDayId(@Param("userId") Long userId, @Param("mealPlanDayId") Long mealPlanDayId);
    void deleteByDayIdAndMealTime(@Param("userId") Long userId, @Param("mealPlanDayId") Long mealPlanDayId, @Param("mealTime") String mealTime);
    
    // For dashboard summary calculation
    List<MealIntake> findConsumedByDayId(@Param("userId") Long userId, @Param("mealPlanDayId") Long mealPlanDayId);
}

package com.dietcoach.project.mapper.meal;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;

@Mapper
public interface MealPlanMapper {

    int insertMealPlan(MealPlan mealPlan);
    int insertMealPlanDay(MealPlanDay day);
    int insertMealItem(MealItem item);

    MealPlan findMealPlanById(@Param("id") Long id);
    List<MealPlanDay> findMealPlanDaysByPlanId(@Param("mealPlanId") Long mealPlanId);
    List<MealItem> findMealItemsByDayId(@Param("mealPlanDayId") Long mealPlanDayId);
    MealPlanDay findMealPlanDayById(@Param("dayId") Long dayId);
    MealPlan findLatestMealPlanByUserId(@Param("userId") Long userId);
    List<MealPlanIngredientResponse> findIngredientsForPlan(@Param("planId") Long planId);
    Integer sumMealItemCaloriesByDayId(@Param("mealPlanDayId") Long mealPlanDayId);
    int updateMealPlanDayTotalCalories(@Param("dayId") Long dayId,
            @Param("totalCalories") Integer totalCalories);
    List<MealPlanIngredientResponse> findIngredientsForPlanInRange(
    	    @Param("planId") Long planId,
    	    @Param("fromDate") LocalDate fromDate,
    	    @Param("toDate") LocalDate toDate
    	);
}

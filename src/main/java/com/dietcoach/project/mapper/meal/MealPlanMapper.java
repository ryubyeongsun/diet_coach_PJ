package com.dietcoach.project.mapper.meal;

import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MealPlanMapper {

    // insert
    void insertMealPlan(MealPlan mealPlan);
    void insertMealPlanDay(MealPlanDay day);
    void insertMealItem(MealItem item);

    // select
    MealPlan findMealPlanById(Long id);
    List<MealPlanDay> findMealPlanDaysByPlanId(Long mealPlanId);
    List<MealItem> findMealItemsByDayId(Long mealPlanDayId);

    MealPlan findLatestMealPlanByUserId(Long userId);
}

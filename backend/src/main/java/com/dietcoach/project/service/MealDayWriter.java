package com.dietcoach.project.service;

import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.mapper.meal.MealPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** A separate bean ensures the transaction proxy also runs on background threads. */
@Service
@RequiredArgsConstructor
public class MealDayWriter {
    private final MealPlanMapper mealPlanMapper;

    @Transactional
    public void save(MealPlanDay day, List<MealItem> items) {
        for (MealItem item : items) {
            mealPlanMapper.insertMealItem(item);
        }
        int calories = items.stream().mapToInt(MealItem::getCalories).sum();
        if (mealPlanMapper.updateMealPlanDayTotalCalories(day.getId(), calories) != 1) {
            throw new IllegalStateException("식단 일자 합계를 저장할 수 없습니다. id=" + day.getId());
        }
        day.setTotalCalories(calories);
    }
}

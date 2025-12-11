package com.dietcoach.project.service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.common.TdeeCalculator;
import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealItemResponse;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.meal.MealPlanMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MealPlanServiceImpl implements MealPlanService {

    private static final int DEFAULT_PLAN_DAYS = 30;

    private final UserMapper userMapper;
    private final MealPlanMapper mealPlanMapper;

    @Override
    @Transactional
    public MealPlanOverviewResponse createMonthlyPlan(Long userId, LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId);
        }

        if (user.getBmr() == null || user.getTdee() == null || user.getTargetCalories() == null) {
            TdeeCalculator.fillUserEnergyFields(user);
            userMapper.updateUserEnergy(user);
        }

        int targetKcalPerDay = (int) Math.round(user.getTargetCalories());
        LocalDate endDate = startDate.plusDays(DEFAULT_PLAN_DAYS - 1);

        MealPlan mealPlan = MealPlan.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(DEFAULT_PLAN_DAYS)
                .targetCaloriesPerDay(targetKcalPerDay)
                .build();
        mealPlanMapper.insertMealPlan(mealPlan);

        List<MealPlanDay> days = new ArrayList<>();
        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();

        for (int i = 0; i < DEFAULT_PLAN_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);

            int breakfastTarget = (int) Math.round(targetKcalPerDay * 0.3);
            int lunchTarget     = (int) Math.round(targetKcalPerDay * 0.4);
            int dinnerTarget    = targetKcalPerDay - breakfastTarget - lunchTarget;

            MealPlanDay day = MealPlanDay.builder()
                    .mealPlanId(mealPlan.getId())
                    .planDate(date)
                    .dayIndex(i + 1)
                    .totalCalories(0)  // 일단 0, 아래에서 채움
                    .build();
            mealPlanMapper.insertMealPlanDay(day);
            days.add(day);

            List<MealItem> items = new ArrayList<>();

            items.addAll(generateMealItemsForOneMeal(day.getId(), "BREAKFAST", breakfastTarget));
            items.addAll(generateMealItemsForOneMeal(day.getId(), "LUNCH", lunchTarget));
            items.addAll(generateMealItemsForOneMeal(day.getId(), "DINNER", dinnerTarget));

            int totalCaloriesForDay = 0;
            for (MealItem item : items) {
                mealPlanMapper.insertMealItem(item);
                totalCaloriesForDay += item.getCalories();
            }

            day.setTotalCalories(totalCaloriesForDay);
            // total_calories 업데이트 쿼리가 필요하면 Mapper에 update 메서드 추가해서 반영해도 됨

            itemsByDayId.put(day.getId(), items);
        }

        // DTO 변환 (기존 그대로)
        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> {
                    List<MealItem> items = itemsByDayId.getOrDefault(day.getId(), List.of());
                    return MealPlanDaySummaryResponse.from(day, items);
                })
                .collect(Collectors.toList());

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }
    @Getter
    @AllArgsConstructor
    private static class FoodItem {
        private final String name;
        private final int caloriesPer100g;
    }

    @Getter
    @AllArgsConstructor
    private static class FoodPortion {
        private final FoodItem food;
        private final int baseGrams;
    }

    // 간단 템플릿 (1차 버전)
    private static final List<FoodPortion> BREAKFAST_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("오트밀", 380), 40),
            new FoodPortion(new FoodItem("그릭 요거트", 60), 150)
    );

    private static final List<FoodPortion> LUNCH_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("현미밥", 150), 200),
            new FoodPortion(new FoodItem("닭가슴살", 165), 150),
            new FoodPortion(new FoodItem("샐러드", 40), 80)
    );

    private static final List<FoodPortion> DINNER_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("현미밥", 150), 150),
            new FoodPortion(new FoodItem("연어", 200), 120),
            new FoodPortion(new FoodItem("샐러드", 40), 80)
    );


    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getMealPlan(Long planId) {
        MealPlan mealPlan = mealPlanMapper.findMealPlanById(planId);
        if (mealPlan == null) {
            throw new BusinessException("존재하지 않는 식단 플랜입니다.");
        }

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(planId);
        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();
        for (MealPlanDay day : days) {
            List<MealItem> items = mealPlanMapper.findMealItemsByDayId(day.getId());
            itemsByDayId.put(day.getId(), items);
        }

        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> MealPlanDaySummaryResponse.from(
                        day,
                        itemsByDayId.getOrDefault(day.getId(), List.of())
                ))
                .collect(Collectors.toList());

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }
    private List<MealItem> generateMealItemsForOneMeal(
            Long mealPlanDayId,
            String mealTime,
            int targetCaloriesForMeal
    ) {
        List<FoodPortion> template;
        switch (mealTime) {
            case "BREAKFAST" -> template = BREAKFAST_TEMPLATE;
            case "LUNCH"     -> template = LUNCH_TEMPLATE;
            case "DINNER"    -> template = DINNER_TEMPLATE;
            default          -> template = BREAKFAST_TEMPLATE;
        }

        // 기준 총 kcal 계산
        int baseTotalCalories = template.stream()
                .mapToInt(p -> p.getBaseGrams() * p.getFood().getCaloriesPer100g() / 100)
                .sum();

        double scale = baseTotalCalories > 0
                ? (double) targetCaloriesForMeal / baseTotalCalories
                : 1.0;

        List<MealItem> result = new ArrayList<>();
        int totalCalories = 0;

        for (FoodPortion portion : template) {
            int scaledGrams = (int) Math.round(portion.getBaseGrams() * scale);

            int itemCalories = scaledGrams * portion.getFood().getCaloriesPer100g() / 100;
            totalCalories += itemCalories;

            MealItem item = MealItem.builder()
                    .mealPlanDayId(mealPlanDayId)
                    .mealTime(mealTime)
                    .foodName(portion.getFood().getName())
                    .grams(scaledGrams)
                    .calories(itemCalories)
                    .memo("자동 생성 식단")
                    .build();
            result.add(item);
        }

        // 혹시 총칼이 너무 찌그러졌으면, 마지막 항목에 약간 보정 넣을 수도 있음(선택)
        // 여기서는 1차 버전이라 패스

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getLatestMealPlanForUser(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) {
            throw new BusinessException("해당 유저의 최근 식단 플랜이 없습니다.");
        }
        return getMealPlan(latestPlan.getId());
    }

    // 🔽 3-1. 쇼핑 연동용 재료 리스트
    @Override
    @Transactional(readOnly = true)
    public List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId) {
        MealPlan plan = mealPlanMapper.findMealPlanById(planId);
        if (plan == null) {
            throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);
        }

        // PRD 버전: 집계는 Mapper(SQL)에서 처리
        List<MealPlanIngredientResponse> list = mealPlanMapper.findIngredientsForPlan(planId);
        if (list.isEmpty()) {
            // 플랜은 있지만 아이템이 없는 경우
            throw new BusinessException("해당 플랜에 재료 정보가 없습니다. id=" + planId);
        }
        return list;
    }

    private void addIngredientsForItem(Map<String, Integer> map, MealItem item) {
        // 더미 규칙: 메뉴 이름별 재료 g 수
        switch (item.getFoodName()) {
            case "닭가슴살 샐러드" -> addGram(map, "닭가슴살", 150);
            case "현미밥 + 닭가슴살" -> {
                addGram(map, "닭가슴살", 150);
                addGram(map, "현미밥", 200);
            }
            case "오트밀 요거트" -> addGram(map, "오트밀", 80);
            default -> {
                // 기타 메뉴는 일단 무시
            }
        }
    }

    private void addGram(Map<String, Integer> map, String ingredient, int gram) {
        map.merge(ingredient, gram, Integer::sum);
    }

    // 🔽 3-2. 대시보드 요약
    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) {
            throw new BusinessException("해당 유저의 최근 식단 플랜이 없습니다.");
        }

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(latestPlan.getId());
        if (days.isEmpty()) {
            throw new BusinessException("해당 플랜에 포함된 날짜가 없습니다.");
        }

        int totalCalories = days.stream()
                .mapToInt(MealPlanDay::getTotalCalories)
                .sum();
        int averageCalories = totalCalories / days.size();

        int targetPerDay = latestPlan.getTargetCaloriesPerDay();
        int achievementRate = (int) Math.round(
                totalCalories / (double) (targetPerDay * days.size()) * 100.0
        );

        return DashboardSummaryResponse.builder()
                .userId(userId)
                .recentMealPlanId(latestPlan.getId())
                .startDate(latestPlan.getStartDate())
                .endDate(latestPlan.getEndDate())
                .totalDays(latestPlan.getTotalDays())
                .targetCaloriesPerDay(targetPerDay)
                .averageCalories(averageCalories)
                .achievementRate(achievementRate)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public MealPlanDayDetailResponse getDayDetail(Long dayId) {
        MealPlanDay day = mealPlanMapper.findMealPlanDayById(dayId);
        if (day == null) {
            throw new BusinessException("존재하지 않는 Day 입니다. id=" + dayId);
        }

        List<MealItem> items = mealPlanMapper.findMealItemsByDayId(dayId);

        List<MealItemResponse> itemResponses = items.stream()
                .map(it -> MealItemResponse.builder()
                        .id(it.getId())
                        .mealTime(it.getMealTime())
                        .foodName(it.getFoodName())
                        .calories(it.getCalories())
                        .grams(it.getGrams())
                        .memo(it.getMemo())
                        .build())
                .toList();

        return MealPlanDayDetailResponse.builder()
                .dayId(day.getId())
                .date(day.getPlanDate().toString())
                .totalCalories(day.getTotalCalories())
                .items(itemResponses)   // 여기 타입도 맞음
                .build();
    }
}

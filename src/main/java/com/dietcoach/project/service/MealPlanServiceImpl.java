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
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.meal.MealPlanMapper;

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
            throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);
        }

        if (user.getBmr() == null || user.getTdee() == null || user.getTargetCalories() == null) {
            TdeeCalculator.fillUserEnergyFields(user);
            userMapper.updateUserEnergy(user);
        }

        int targetKcalPerDay = (int) Math.round(user.getTargetCalories());
        LocalDate endDate = startDate.plusDays(DEFAULT_PLAN_DAYS - 1);

        // 2) meal_plans 생성
        MealPlan mealPlan = MealPlan.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(DEFAULT_PLAN_DAYS)
                .targetCaloriesPerDay(targetKcalPerDay)
                .build();
        mealPlanMapper.insertMealPlan(mealPlan);

        // 3) 30일 루프 돌면서 day + item 생성
        List<MealPlanDay> days = new ArrayList<>();
        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();

        for (int i = 0; i < DEFAULT_PLAN_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);

            int breakfastKcal = (int) (targetKcalPerDay * 0.3);
            int lunchKcal = (int) (targetKcalPerDay * 0.4);
            int dinnerKcal = targetKcalPerDay - breakfastKcal - lunchKcal;

            MealPlanDay day = MealPlanDay.builder()
                    .mealPlanId(mealPlan.getId())
                    .planDate(date)
                    .dayIndex(i + 1)
                    .totalCalories(breakfastKcal + lunchKcal + dinnerKcal)
                    .build();
            mealPlanMapper.insertMealPlanDay(day);
            days.add(day);

            List<MealItem> items = new ArrayList<>();

            // 🔽 음식 이름을 재료 기반으로 더미 설정
            items.add(MealItem.builder()
                    .mealPlanDayId(day.getId())
                    .mealTime("BREAKFAST")
                    .foodName("닭가슴살 샐러드")
                    .calories(breakfastKcal)
                    .memo("더미 데이터")
                    .build());

            items.add(MealItem.builder()
                    .mealPlanDayId(day.getId())
                    .mealTime("LUNCH")
                    .foodName("현미밥 + 닭가슴살")
                    .calories(lunchKcal)
                    .memo("더미 데이터")
                    .build());

            items.add(MealItem.builder()
                    .mealPlanDayId(day.getId())
                    .mealTime("DINNER")
                    .foodName("오트밀 요거트")
                    .calories(dinnerKcal)
                    .memo("더미 데이터")
                    .build());

            for (MealItem item : items) {
                mealPlanMapper.insertMealItem(item);
            }
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

        List<MealPlanDayDetailResponse.MealItemResponse> itemResponses = items.stream()
                .map(it -> MealPlanDayDetailResponse.MealItemResponse.builder()
                        .mealTime(it.getMealTime())
                        .foodName(it.getFoodName())
                        .calories(it.getCalories())
                        .memo(it.getMemo())
                        .build())
                .toList();

        return MealPlanDayDetailResponse.builder()
                .dayId(day.getId())
                .date(day.getPlanDate() != null ? day.getPlanDate().toString() : null)
                .totalCalories(day.getTotalCalories())
                .items(itemResponses)
                .build();
    }
}

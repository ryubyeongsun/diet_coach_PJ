package com.dietcoach.project.service;

import com.dietcoach.project.common.TdeeCalculator;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.meal.MealPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

        // 1) 유저 조회 + TDEE/목표칼로리 보장
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId);
        }

        if (user.getBmr() == null || user.getTdee() == null || user.getTargetCalories() == null) {
            TdeeCalculator.fillUserEnergyFields(user);
            userMapper.updateUserEnergy(user); // 이 메서드 만들었는지도 한번 확인!
        }

        int targetKcalPerDay = (int) Math.round(user.getTargetCalories());   // 🔹 수정
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

        // 3) 30일 루프 돌면서 day + item 더미 생성
        List<MealPlanDay> days = new ArrayList<>();
        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();

        for (int i = 0; i < DEFAULT_PLAN_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);

            // 단순 규칙: 아침 30%, 점심 40%, 저녁 30%
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

            items.add(MealItem.builder()
                    .mealPlanDayId(day.getId())
                    .mealTime("BREAKFAST")
                    .foodName("자동 생성 아침 식단")
                    .calories(breakfastKcal)
                    .memo("더미 데이터")
                    .build());

            items.add(MealItem.builder()
                    .mealPlanDayId(day.getId())
                    .mealTime("LUNCH")
                    .foodName("자동 생성 점심 식단")
                    .calories(lunchKcal)
                    .memo("더미 데이터")
                    .build());

            items.add(MealItem.builder()
                    .mealPlanDayId(day.getId())
                    .mealTime("DINNER")
                    .foodName("자동 생성 저녁 식단")
                    .calories(dinnerKcal)
                    .memo("더미 데이터")
                    .build());

            for (MealItem item : items) {
                mealPlanMapper.insertMealItem(item);
            }

            itemsByDayId.put(day.getId(), items);
        }

        // 4) DTO 매핑
        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> {
                    List<MealItem> items = itemsByDayId.getOrDefault(day.getId(), List.of());
                    // DTO 쪽에 from(day, items) 같은 팩토리 메서드 만들어 쓰면 깔끔
                    return MealPlanDaySummaryResponse.from(day, items);
                })
                .collect(Collectors.toList());

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getMealPlan(Long planId) {
        MealPlan mealPlan = mealPlanMapper.findMealPlanById(planId);
        if (mealPlan == null) {
            throw new IllegalArgumentException("존재하지 않는 식단 플랜입니다. id=" + planId);
        }

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(planId);

        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();
        for (MealPlanDay day : days) {
            List<MealItem> items = mealPlanMapper.findMealItemsByDayId(day.getId());
            itemsByDayId.put(day.getId(), items);
        }

        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> MealPlanDaySummaryResponse.from(
                        day, itemsByDayId.getOrDefault(day.getId(), List.of())
                ))
                .collect(Collectors.toList());

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getLatestMealPlanForUser(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) {
            return null; // 컨트롤러에서 적절히 처리
        }
        return getMealPlan(latestPlan.getId());
    }
}

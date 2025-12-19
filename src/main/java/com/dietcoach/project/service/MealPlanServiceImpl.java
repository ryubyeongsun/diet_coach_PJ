package com.dietcoach.project.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.common.TdeeCalculator;
import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.WeightRecord;
import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealItemResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.WeightRecordMapper;
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
    private final WeightRecordMapper weightRecordMapper;

    // =========================
    // A-2 식단 생성용 간단 카탈로그/템플릿
    // =========================
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

    // =========================
    // ✅ 1) 한 달 식단 생성 (단일 진입점)
    // =========================
    @Override
    @Transactional
    public MealPlanOverviewResponse createMonthlyPlan(Long userId, MealPlanCreateRequest request) {
        if (userId == null) throw new BusinessException("userId is required");

        LocalDate startDate = (request != null) ? request.getStartDate() : null;
        if (startDate == null) startDate = LocalDate.now();

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);
        }

        // 에너지 필드가 비어있으면 채우고 저장
        if (user.getBmr() == null || user.getTdee() == null || user.getTargetCalories() == null) {
            TdeeCalculator.fillUserEnergyFields(user);
            userMapper.updateUserEnergy(user);
        }

        int targetKcalPerDay = (int) Math.round(user.getTargetCalories());
        LocalDate endDate = startDate.plusDays(DEFAULT_PLAN_DAYS - 1);

        // ✅ A1 필드 처리(오늘 목표: 저장/전달)
        Long monthlyBudget = (request != null) ? request.getMonthlyBudget() : null;
        Integer mealsPerDay = (request != null) ? request.getMealsPerDay() : null;

        String preferencesCsv = toCsv((request != null) ? request.getPreferences() : null);
        String allergiesCsv   = toCsv((request != null) ? request.getAllergies() : null);

        MealPlan mealPlan = MealPlan.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(DEFAULT_PLAN_DAYS)
                .targetCaloriesPerDay(targetKcalPerDay)

                // ✅ A1 저장 필드
                .monthlyBudget(monthlyBudget)
                .mealsPerDay(mealsPerDay)
                .preferences(preferencesCsv)
                .allergies(allergiesCsv)

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
                    .totalCalories(0)
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
            mealPlanMapper.updateMealPlanDayTotalCalories(day.getId(), totalCaloriesForDay);

            itemsByDayId.put(day.getId(), items);
        }

        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> MealPlanDaySummaryResponse.from(
                        day,
                        itemsByDayId.getOrDefault(day.getId(), List.of())
                ))
                .toList();

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }

    private String toCsv(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private List<MealItem> generateMealItemsForOneMeal(Long mealPlanDayId, String mealTime, int targetCaloriesForMeal) {
        List<FoodPortion> template = switch (mealTime) {
            case "BREAKFAST" -> BREAKFAST_TEMPLATE;
            case "LUNCH" -> LUNCH_TEMPLATE;
            case "DINNER" -> DINNER_TEMPLATE;
            default -> BREAKFAST_TEMPLATE;
        };

        int baseTotalCalories = template.stream()
                .mapToInt(p -> p.getBaseGrams() * p.getFood().getCaloriesPer100g() / 100)
                .sum();

        double scale = baseTotalCalories > 0
                ? (double) targetCaloriesForMeal / baseTotalCalories
                : 1.0;

        List<MealItem> result = new ArrayList<>();
        for (FoodPortion portion : template) {
            int scaledGrams = Math.max(1, (int) Math.round(portion.getBaseGrams() * scale));
            int itemCalories = scaledGrams * portion.getFood().getCaloriesPer100g() / 100;

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

        return result;
    }

    // =========================
    // 2) 식단 플랜 상세 조회
    // =========================
    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getMealPlan(Long planId) {
        MealPlan mealPlan = mealPlanMapper.findMealPlanById(planId);
        if (mealPlan == null) {
            throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);
        }

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(planId);

        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();
        for (MealPlanDay day : days) {
            itemsByDayId.put(day.getId(), mealPlanMapper.findMealItemsByDayId(day.getId()));
        }

        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> MealPlanDaySummaryResponse.from(
                        day,
                        itemsByDayId.getOrDefault(day.getId(), List.of())
                ))
                .toList();

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }

    // =========================
    // 3) 최신 식단 플랜 조회
    // =========================
    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getLatestMealPlanForUser(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) {
            throw new BusinessException("해당 유저의 최근 식단 플랜이 없습니다. userId=" + userId);
        }
        return getMealPlan(latestPlan.getId());
    }

    // =========================
    // 4) 재료 집계
    // =========================
    @Override
    @Transactional(readOnly = true)
    public List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId) {
        MealPlan plan = mealPlanMapper.findMealPlanById(planId);
        if (plan == null) {
            throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);
        }

        List<MealPlanIngredientResponse> list = mealPlanMapper.findIngredientsForPlan(planId);
        if (list == null || list.isEmpty()) {
            throw new BusinessException("해당 플랜에 재료 정보가 없습니다. id=" + planId);
        }
        return list;
    }

    // =========================
    // 5) 대시보드 요약
    // =========================
    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) {
            throw new BusinessException("해당 유저의 최근 식단 플랜이 없습니다. userId=" + userId);
        }

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(latestPlan.getId());
        if (days == null || days.isEmpty()) {
            throw new BusinessException("해당 플랜에 포함된 날짜가 없습니다. planId=" + latestPlan.getId());
        }

        int totalCalories = days.stream().mapToInt(MealPlanDay::getTotalCalories).sum();
        int averageCalories = (int) Math.round(totalCalories / (double) days.size());

        int targetPerDay = latestPlan.getTargetCaloriesPerDay();
        int achievementRate = (int) Math.round(
                totalCalories / (double) (targetPerDay * days.size()) * 100.0
        );

        WeightRecord latest = weightRecordMapper.findLatestByUserId(userId);

        boolean hasWeightRecords = latest != null;
        Double latestWeight = (latest != null) ? latest.getWeight() : null;

        Double weightChange7Days = null;
        if (latest != null) {
            LocalDate d7 = latest.getRecordDate().minusDays(7);
            WeightRecord weight7 = weightRecordMapper.findByUserIdAndDate(userId, d7);
            if (weight7 != null) {
                weightChange7Days = latest.getWeight() - weight7.getWeight();
            }
        }

        return DashboardSummaryResponse.builder()
                .userId(userId)
                .recentMealPlanId(latestPlan.getId())
                .startDate(latestPlan.getStartDate())
                .endDate(latestPlan.getEndDate())
                .totalDays(latestPlan.getTotalDays())
                .targetCaloriesPerDay(targetPerDay)
                .averageCalories(averageCalories)
                .achievementRate(achievementRate)
                .hasWeightRecords(hasWeightRecords)
                .latestWeight(latestWeight)
                .weightChange7Days(weightChange7Days)
                .build();
    }

    // =========================
    // 6) 하루 상세 조회
    // =========================
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
                .items(itemResponses)
                .build();
    }
}

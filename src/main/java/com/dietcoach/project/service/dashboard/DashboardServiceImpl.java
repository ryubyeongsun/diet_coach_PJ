package com.dietcoach.project.service.dashboard;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.dto.dashboard.DashboardTrendResponse;
import com.dietcoach.project.dto.dashboard.DayTrend;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.dashboard.DashboardMapper;
import com.dietcoach.project.mapper.meal.MealPlanMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final UserMapper userMapper;
    private final MealPlanMapper mealPlanMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardTrendResponse getTrend(Long userId, LocalDate from, LocalDate to) {
        if (userId == null) {
            throw new BusinessException("userId is required");
        }

        // 1) 기본값: 최근 30일
        LocalDate today = LocalDate.now();
        if (to == null) to = today;
        if (from == null) from = to.minusDays(29);

        if (from.isAfter(to)) {
            throw new BusinessException("from must be <= to");
        }

        // 2) 유저 확인 + 목표칼로리 계산
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);
        }

        Integer userTarget = (user.getTargetCalories() == null)
                ? null
                : (int) Math.round(user.getTargetCalories());

        // 3) 최신 식단 플랜 조회 (식단 기간 클리핑)
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) {
            // 식단 자체가 없으면 -> hasData=false, 빈 리스트 (500 금지)
            return DashboardTrendResponse.builder()
                    .hasData(false)
                    .fromDate(from.toString())
                    .toDate(to.toString())
                    .dayTrends(List.of())
                    .build();
        }

        LocalDate planStart = latestPlan.getStartDate();
        LocalDate planEnd = latestPlan.getEndDate();

        // 요청 기간을 식단 기간으로 클리핑
        LocalDate clippedFrom = (from.isBefore(planStart)) ? planStart : from;
        LocalDate clippedTo = (to.isAfter(planEnd)) ? planEnd : to;

        // 식단 기간과 완전히 불일치하면 -> 빈 응답
        if (clippedFrom.isAfter(clippedTo)) {
            return DashboardTrendResponse.builder()
                    .hasData(false)
                    .fromDate(clippedFrom.toString())
                    .toDate(clippedTo.toString())
                    .dayTrends(List.of())
                    .build();
        }

        // targetCalories 우선순위: users.target_calories -> meal_plans.target_calories_per_day
        Integer targetCalories = (userTarget != null) ? userTarget : latestPlan.getTargetCaloriesPerDay();

        // 4) 칼로리/체중 조회
        List<DashboardMapper.DayCaloriesRow> caloriesRows =
                dashboardMapper.findDayCalories(userId, clippedFrom, clippedTo);

        List<DashboardMapper.DayWeightRow> weightRows =
                dashboardMapper.findWeights(userId, from, to);

        // Debug logs
        System.out.println("DEBUG: getTrend - userId: " + userId);
        System.out.println("DEBUG: getTrend - original from: " + from + ", to: " + to);
        System.out.println("DEBUG: getTrend - clipped from: " + clippedFrom + ", to: " + clippedTo);
        System.out.println("DEBUG: getTrend - caloriesRows size: " + caloriesRows.size());
        System.out.println("DEBUG: getTrend - weightRows size: " + weightRows.size());

        // 5) date 기준 merge (칼로리/체중 중 하나만 있어도 포함)
        Map<LocalDate, DayTrend> map = new HashMap<>();

        for (DashboardMapper.DayCaloriesRow r : caloriesRows) {
            System.out.println("DEBUG: Processing caloriesRow: " + r.getDate() + ", " + r.getTotalCalories());
            if (r.getDate() == null) continue;
            map.computeIfAbsent(r.getDate(), d -> DayTrend.builder()
                            .date(d.toString())
                            .targetCalories(targetCalories)
                            .build())
                    .setTotalCalories(r.getTotalCalories());
            System.out.println("DEBUG: Map entry after caloriesRow: " + map.get(r.getDate()));
        }

        System.out.println("DEBUG: Before weightRows loop, weightRows size: " + weightRows.size());
        for (DashboardMapper.DayWeightRow r : weightRows) {
            System.out.println("DEBUG: Processing weightRow: " + r.getDate() + ", " + r.getWeight());
            if (r.getDate() == null) continue;
            map.computeIfAbsent(r.getDate(), d -> DayTrend.builder()
                            .date(d.toString())
                            .targetCalories(targetCalories)
                            .build())
                    .setWeight(r.getWeight());
        }
        System.out.println("DEBUG: Map after merging (size: " + map.size() + "): " + map);

        // 6) achievementRate 계산
        for (DayTrend trend : map.values()) {
            Integer tc = trend.getTotalCalories();
            Integer tgt = trend.getTargetCalories();
            if (tc != null && tgt != null && tgt > 0) {
                int rate = (int) Math.round(tc / (double) tgt * 100.0);
                trend.setAchievementRate(rate);
            } else {
                trend.setAchievementRate(null);
            }
        }

        List<DayTrend> dayTrends = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        boolean hasData = !dayTrends.isEmpty();

        return DashboardTrendResponse.builder()
                .hasData(hasData)
                .fromDate(clippedFrom.toString())
                .toDate(clippedTo.toString())
                .dayTrends(dayTrends)
                .build();
    }
}

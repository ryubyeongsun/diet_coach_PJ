package com.dietcoach.project.service.dashboard;

import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.dto.dashboard.DashboardTrendResponse;
import com.dietcoach.project.dto.dashboard.DayTrend;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.dashboard.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardTrendResponse getTrend(Long userId, LocalDate from, LocalDate to) {
        if (userId == null) throw new BusinessException("userId is required");

        LocalDate today = LocalDate.now();
        if (to == null) to = today;
        if (from == null) from = to.minusDays(29);
        if (from.isAfter(to)) throw new BusinessException("from must be <= to");

        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);

        Integer targetCalories = (user.getTargetCalories() == null)
                ? null
                : (int) Math.round(user.getTargetCalories());

        List<DashboardMapper.DayCaloriesRow> caloriesRows = dashboardMapper.findDayCalories(userId, from, to);
        List<DashboardMapper.DayWeightRow> weightRows = dashboardMapper.findWeights(userId, from, to);

        Map<LocalDate, Temp> tempMap = new HashMap<>();

        for (DashboardMapper.DayCaloriesRow r : caloriesRows) {
            if (r.getDate() == null) continue;
            tempMap.computeIfAbsent(r.getDate(), d -> new Temp()).totalCalories = r.getTotalCalories();
        }

        for (DashboardMapper.DayWeightRow r : weightRows) {
            if (r.getDate() == null) continue;
            tempMap.computeIfAbsent(r.getDate(), d -> new Temp()).weight = r.getWeight();
        }

        List<DayTrend> dayTrends = tempMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    LocalDate d = e.getKey();
                    Temp t = e.getValue();

                    Integer achievementRate = null;
                    if (t.totalCalories != null && targetCalories != null && targetCalories > 0) {
                        achievementRate = (int) Math.round(t.totalCalories / (double) targetCalories * 100.0);
                    }

                    return DayTrend.builder()
                            .date(d.toString())
                            .totalCalories(t.totalCalories)
                            .targetCalories(targetCalories)
                            .weight(t.weight)
                            .achievementRate(achievementRate) // 필드 없으면 이 줄 삭제
                            .build();
                })
                .collect(Collectors.toList());

        return DashboardTrendResponse.builder()
                .dayTrends(dayTrends)
                .build();
    }

    private static class Temp {
        Integer totalCalories;
        Double weight;
    }
}

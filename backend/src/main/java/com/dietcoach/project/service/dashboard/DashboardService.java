package com.dietcoach.project.service.dashboard;


import java.time.LocalDate;

import com.dietcoach.project.dto.dashboard.DashboardTrendResponse;

public interface DashboardService {
    DashboardTrendResponse getTrend(Long userId, LocalDate from, LocalDate to);
}

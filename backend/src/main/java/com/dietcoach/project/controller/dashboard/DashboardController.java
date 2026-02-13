package com.dietcoach.project.controller.dashboard;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.dashboard.DashboardTrendResponse;
import com.dietcoach.project.service.dashboard.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/trend?userId=1&from=2025-12-01&to=2025-12-17
     * from/to 없으면 최근 30일
     */
    @GetMapping("/trend")
    public ApiResponse<DashboardTrendResponse> getTrend(
            @RequestParam Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(dashboardService.getTrend(userId, from, to));
    }
}

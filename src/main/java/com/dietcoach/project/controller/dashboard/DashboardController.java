package com.dietcoach.project.controller.dashboard;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.dashboard.DashboardTrendResponse;
import com.dietcoach.project.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/trend")
    public ApiResponse<DashboardTrendResponse> getTrend(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(dashboardService.getTrend(userId, from, to));
    }
}

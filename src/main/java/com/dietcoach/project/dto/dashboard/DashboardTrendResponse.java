package com.dietcoach.project.dto.dashboard;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard Trend 응답
 * - hasData: 데이터 유무(칼로리 or 체중 중 하나라도 있으면 true)
 * - fromDate/toDate: 실제 적용된 조회 기간(식단 기간으로 클리핑 후 값)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTrendResponse {
    private Boolean hasData;
    private String fromDate;
    private String toDate;
    private List<DayTrend> dayTrends;
}

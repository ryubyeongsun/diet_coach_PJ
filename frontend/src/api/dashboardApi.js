// src/api/dashboardApi.js
import api from './http';

export async function fetchDashboardSummary(userId) {
  const res = await api.get('/dashboard/summary', {
    params: { userId },
  });
  return res.data.data;
  // 예: { latestWeight, weightChange7Days, targetCalories, avgCaloriesThisMonth, ... }
}

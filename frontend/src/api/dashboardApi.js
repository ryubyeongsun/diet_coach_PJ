// src/api/dashboardApi.js
import http from './http';

export async function fetchDashboardSummary(userId) {
  const res = await http.get('/dashboard/summary', { params: { userId } }); // ✅
  return res.data.data;
}

export async function fetchDashboardTrend(userId, from, to) {
  const res = await http.get('/dashboard/trend', {
    params: { userId, from, to },
  });
  return res.data.data;
}

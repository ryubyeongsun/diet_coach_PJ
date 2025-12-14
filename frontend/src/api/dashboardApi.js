// src/api/dashboardApi.js
import api from './http';

export async function fetchDashboardSummary(userId) {
  const res = await api.get('/dashboard/summary', { params: { userId } }); // ✅
  return res.data.data;
}

export async function fetchDashboardTrend(userId, from, to) {
  const res = await api.get('/dashboard/trend', {
    params: { userId, from, to }, // from/to는 선택이면 빼도 됨
  });
  return res.data.data;
}

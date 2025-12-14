// src/api/dashboardApi.js
import api from "./http";

export async function fetchDashboardSummary(userId) {
  const res = await api.get(`/users/${userId}/dashboard-summary`);
  return res.data.data;
}

export async function fetchDashboardTrend({ userId, from, to } = {}) {
  const res = await api.get(`/dashboard/trend`, {
    params: { userId, from, to },
  });
  return res.data.data;
}

export async function fetchDashboardTrend(userId, { from, to } = {}) {
  const res = await api.get('/dashboard/trend', {
    params: { userId, from, to },
  });
  return res.data.data; // { dayTrends: [...] }
}

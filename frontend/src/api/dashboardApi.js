// src/api/dashboardApi.js
import api from "./http";

export async function fetchDashboardSummary(userId) {
  const res = await api.get(`/users/${userId}/dashboard-summary`);
  return res.data.data;
}

<<<<<<< HEAD
export async function fetchDashboardTrend({ userId, from, to } = {}) {
  const res = await api.get(`/dashboard/trend`, {
    params: { userId, from, to },
  });
  return res.data.data;
=======
export async function fetchDashboardTrend(userId, { from, to } = {}) {
  const res = await api.get('/dashboard/trend', {
    params: { userId, from, to },
  });
  return res.data.data; // { dayTrends: [...] }
>>>>>>> 663b276177ac4ed06dbd33c09f3e1dc55401d188
}

// src/api/mealPlanApi.js
import api from './http';

// 최신 식단 플랜 조회
export async function fetchLatestMealPlan(userId) {
  const res = await api.get(`/users/${userId}/meal-plans/latest`);
  return res.data.data; // MealPlanOverviewResponse
}

// 한 달 식단 생성
export async function generateMealPlan(payload) {
  const res = await api.post('/meal-plans', payload);
  return res.data.data; // 생성된 MealPlanOverviewResponse or planId
}

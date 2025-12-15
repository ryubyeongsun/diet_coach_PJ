// src/api/mealPlanApi.js
import http from './http';

// 최신 식단 플랜 조회
export async function fetchLatestMealPlan(userId) {
  const res = await http.get(`/users/${userId}/meal-plans/latest`);
  return res.data.data ?? res.data;
}

// 식단 플랜 상세 조회
export async function fetchMealPlan(planId) {
  const res = await http.get(`/meal-plans/${planId}`);
  return res.data.data ?? res.data;
}

// 한 달 식단 생성
export async function generateMealPlan(payload) {
  const res = await http.post('/meal-plans', payload);
  return res.data.data ?? res.data;
}

// 하루 상세 식단 조회
export async function fetchDayDetail(dayId) {
  const res = await http.get(`/meal-plans/days/${dayId}`);
  return res.data.data ?? res.data;
}

// 한 달 식단 재료 요약 조회
export async function fetchPlanIngredients(planId) {
  const res = await http.get(`/meal-plans/${planId}/ingredients`);
  return res.data.data ?? res.data;
}

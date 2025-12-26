// src/api/mealPlanApi.js
import http from "./http";

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

// 30일 식단 생성 (PRD 기반)
export async function createMealPlan(payload) {
  // AI 생성 시간이 길어질 수 있으므로 타임아웃을 3분(180000ms)으로 연장
  const res = await http.post('/meal-plans', payload, { timeout: 180000 });
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

// 하루 식단 재생성 (A4)
export async function regenerateDay(dayId) {
  const res = await http.post(`/meal-plans/days/${dayId}/regenerate`, {}, { timeout: 180000 });
  return res.data.data ?? res.data;
}

// 특정 끼니 교체 (A4)
export async function replaceMeal(dayId, mealTime) {
  const res = await http.post(`/meal-plans/days/${dayId}/meals/${mealTime}/replace`, {}, { timeout: 180000 });
  return res.data.data ?? res.data;
}

// 식단 도장 찍기
export async function stampMealPlanDay(dayId) {
  const res = await http.post(`/meal-plans/days/${dayId}/stamp`);
  return res?.data;
}

// 섭취 체크 (upsert)
export async function upsertIntake(payload) {
  const res = await http.put('/meal-intakes', payload);
  return res.data.data ?? res.data;
}

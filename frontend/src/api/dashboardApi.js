// src/api/dashboardApi.js
import http from './http';

/**
 * 대시보드 요약 정보를 조회합니다.
 * @returns {Promise<object>}
 */
export async function getDashboardSummary() {
  // PRD에 명시된 대로, API 경로가 변경될 수 있음을 가정하고 여기서 관리합니다.
  // 현재는 /api/dashboard/summary를 사용합니다.
  const res = await http.get('/dashboard/summary');
  return res.data;
}

/**
 * 대시보드 트렌드 데이터를 조회합니다.
 * @param {object} params
 * @param {number} params.userId
 * @param {string} params.from - YYYY-MM-DD
 * @param {string} params.to - YYYY-MM-DD
 * @returns {Promise<object>}
 */
export async function getDashboardTrend({ userId, from, to }) {
  const res = await http.get('/dashboard/trend', {
    params: { userId, from, to },
  });
  return res.data;
}
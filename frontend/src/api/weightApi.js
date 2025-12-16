// src/api/weightApi.js
import http from './http';

// 오늘 체중 기록 생성/업데이트
export async function upsertWeight(userId, payload) {
  // payload: { recordDate: '2025-12-12', weight: 73.2, memo: 'string | null' }
  const res = await http.post(`/users/${userId}/weights`, payload);
  return res.data.data; // 저장된 WeightRecordResponse
}

// 기간별 체중 목록 조회
export async function fetchWeights(userId, params = {}) {
  // params: { from?: 'YYYY-MM-DD', to?: 'YYYY-MM-DD' }
  const res = await http.get(`/users/${userId}/weights`, { params });
  return res.data; // WeightRecordResponse[] 또는 { records: [...] } 형태면 그에 맞게
}

// 특정 체중 기록 삭제
export async function deleteWeight(userId, recordId) {
  const res = await http.delete(`/users/${userId}/weights/${recordId}`);
  return res.data;
}

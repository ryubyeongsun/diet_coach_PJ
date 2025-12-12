// src/api/weightApi.js
import api from './http';

// 오늘 체중 기록 생성/업데이트
export async function createWeight(userId, payload) {
  // payload: { recordDate: '2025-12-12', weight: 73.2, memo: 'string | null' }
  const res = await api.post(`/users/${userId}/weights`, payload);
  return res.data.data; // 저장된 WeightRecordResponse
}

// 기간별 체중 목록 조회
export async function fetchWeights(userId, params = {}) {
  // params: { from?: 'YYYY-MM-DD', to?: 'YYYY-MM-DD' }
  const res = await api.get(`/users/${userId}/weights`, { params });
  return res.data.data; // WeightRecordResponse[] 또는 { records: [...] } 형태면 그에 맞게
}

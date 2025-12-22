// src/api/weightApi.js
import http from "./http";

/**
 * 특정 기간 동안의 체중 기록을 조회합니다.
 * @param {number} userId - 사용자 ID
 * @param {object} params - 조회 기간
 * @param {string} params.from - 시작일 (YYYY-MM-DD)
 * @param {string} params.to - 종료일 (YYYY-MM-DD)
 * @returns {Promise<object>} - 체중 기록 리스트
 */
export async function getWeights(userId, { from, to }) {
  const res = await http.get(`/users/${userId}/weights`, {
    params: { from, to },
  });
  return res.data;
}

/**
 * 특정 날짜의 체중을 등록하거나 수정합니다.
 * @param {number} userId - 사용자 ID
 * @param {object} payload - 전송할 데이터
 * @param {string} payload.recordDate - 기록일 (YYYY-MM-DD)
 * @param {number} payload.weight - 체중
 * @returns {Promise<object>} - 처리 결과
 */
export async function upsertWeight(userId, payload) {
  const res = await http.post(`/users/${userId}/weights`, payload);
  return res.data;
}

/**
 * 특정 체중 기록을 삭제합니다.
 * @param {number} userId - 사용자 ID
 * @param {number} recordId - 삭제할 기록의 ID
 * @returns {Promise<void>}
 */
export async function deleteWeight(userId, recordId) {
  await http.delete(`/users/${userId}/weights/${recordId}`);
}

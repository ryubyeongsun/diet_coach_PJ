// src/api/userApi.js
import api from './http';

export async function createUser(payload) {
  const res = await api.post('/users', payload);
  return res.data.data; // ApiResponse<T> 구조라고 가정
}

export async function getUser(id) {
  const res = await api.get(`/users/${id}`);
  return res.data.data;
}

/**
 * 임시로 사용할 유저 초기화 함수.
 * 실제 프로덕션에서는 로그인/세션 처리를 통해 유저 정보를 가져와야 합니다.
 * 지금은 테스트를 위해 하드코딩된 유저 ID(1)를 반환합니다.
 * @returns {Promise<number>} 유저 ID
 */
export async function initUser() {
  console.log('[usersApi] DEV: Using hardcoded user ID 1');
  return Promise.resolve(1);
}

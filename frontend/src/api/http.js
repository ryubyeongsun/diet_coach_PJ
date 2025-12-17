// src/api/http.js
import axios from 'axios';
import { getToken, clearAuth } from '../utils/auth';
import { setLoading } from '../utils/globalState';

const http = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

// Request interceptor
http.interceptors.request.use(
  (config) => {
    setLoading(true);
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    setLoading(false);
    return Promise.reject(error);
  }
);

// Response interceptor
http.interceptors.response.use(
  (response) => {
    setLoading(false);
    // 백엔드에서 success: false 응답을 보내는 경우를 위한 처리
    if (response.data && response.data.success === false) {
      return Promise.reject(new Error(response.data.message || '오류가 발생했습니다.'));
    }
    return response.data;
  },
  (error) => {
    setLoading(false);

    if (error.response) {
      const { status } = error.response;

      if (status === 401) {
        clearAuth(); // 토큰 및 사용자 정보 삭제
        alert('로그인 시간이 만료되었습니다.\n다시 로그인해 주세요.');
        // SPA 라우팅이 아닌 전체 페이지 리로드로 상태를 초기화
        window.location.href = '/login';
      } else if (status >= 500) {
        // 5xx 서버 에러 공통 처리
        alert('서버 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요.');
      }
    } else {
      // 네트워크 에러 등 response가 없는 경우
      alert('네트워크 오류가 발생했습니다.\n연결을 확인해 주세요.');
    }
    
    return Promise.reject(error);
  }
);

export default http;

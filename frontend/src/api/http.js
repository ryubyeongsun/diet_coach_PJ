// src/api/http.js
import axios from 'axios';
import { getToken } from '../utils/auth';
import { setLoading, setError } from '../utils/globalState';

const http = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

// Request interceptor
http.interceptors.request.use(
  (config) => {
    setLoading(true); // 로딩 시작
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    setLoading(false); // 에러 시 로딩 종료
    return Promise.reject(error);
  }
);

// Response interceptor
http.interceptors.response.use(
  (response) => {
    setLoading(false); // 응답 시 로딩 종료
    if (response.data && response.data.success === false) {
      setError(response.data.message || '오류가 발생했습니다.');
      return Promise.reject(new Error(response.data.message));
    }
    return response.data; // 실제 데이터만 반환하도록 변경
  },
  (error) => {
    setLoading(false); // 에러 시 로딩 종료
    
    let errorMessage = '네트워크 오류 또는 서버에 문제가 발생했습니다.';
    if (error.response) {
      // 서버에서 보낸 에러 메시지가 있는 경우
      errorMessage = error.response.data?.message || errorMessage;
      if (error.response.status === 401 || error.response.status === 403) {
        errorMessage = '인증에 실패했습니다. 다시 로그인해주세요.';
        // 필요시 로그인 페이지로 리다이렉트하는 로직 추가 가능
      }
    }
    setError(errorMessage);
    return Promise.reject(error);
  }
);

export default http;

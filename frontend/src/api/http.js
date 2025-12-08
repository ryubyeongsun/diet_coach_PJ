// src/api/http.js
import axios from 'axios';

// 백엔드 스프링 서버 기준
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 5000,
});

// 나중에 요청/응답 인터셉터 추가 가능
export default api;

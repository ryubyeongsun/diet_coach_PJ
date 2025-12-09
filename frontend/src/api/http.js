// src/api/http.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 5000,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[API ERROR]', error.response || error);
    return Promise.reject(error);
  }
);

export default api;

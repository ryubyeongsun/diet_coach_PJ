// src/api/http.js
import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

// Request interceptor for logging
api.interceptors.request.use(
  (config) => {
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response) => {
    // Assuming backend always returns an object with a 'success' property
    if (response.data && response.data.success === false) {
      // If backend indicates failure, throw an error with the message
      throw new Error(response.data.message || 'Backend operation failed');
    }
    return response;
  },
  (error) => {
    console.error('[API Error]', error.response?.data || error.message || error);
    // If it's an HTTP error (e.g., 4xx, 5xx), the error object will typically have a response property
    // We re-throw the error so it can be caught by the caller
    return Promise.reject(error);
  }
);

export default api;

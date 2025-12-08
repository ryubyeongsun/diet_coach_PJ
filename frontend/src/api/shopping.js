// src/api/shopping.js
import api from './http';

// 상품 검색
export async function searchProducts(keyword) {
  const res = await api.get('/shopping/search', {
    params: { keyword },
  });
  // 스프링 ApiResponse<T> 구조: { success, message, data }
  return res.data;
}

// 재료 + 필요 그램 수 기반 추천
export async function recommendProducts(ingredient, neededGram) {
  const res = await api.get('/shopping/recommendations', {
    params: {
      ingredient,
      neededGram,
    },
  });
  return res.data;
}

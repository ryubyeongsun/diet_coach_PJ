// src/api/shoppingApi.js
import api from './http';

// 검색 API: /api/shopping/search?keyword=...
export async function searchProducts(keyword) {
  const res = await api.get('/shopping/search', {
    params: { keyword },
  });
  return res.data; // ShoppingProduct 리스트
}

// 추천 API: /api/shopping/recommendations?ingredient=..&neededGram=..
export async function getRecommendations(ingredient, neededGram) {
  const res = await api.get('/shopping/recommendations', {
    params: { ingredient, neededGram },
  });
  return res.data; // 추천 상품 리스트
}

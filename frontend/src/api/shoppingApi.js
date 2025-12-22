// src/api/shoppingApi.js
import http from "./http";

// 검색 API: /api/shopping/search?keyword=...
export async function searchProducts(keyword) {
  const res = await http.get("/shopping/search", {
    params: { keyword },
  });
  return res.data; // ShoppingProduct 리스트
}

// 추천 API: /api/shopping/recommendations?ingredient=..&neededGram=..
export async function getRecommendations(ingredient, neededGram) {
  const res = await http.get("/shopping/recommendations", {
    params: { ingredient, neededGram },
  });
  return res.data; // 추천 상품 리스트
}

/**
 * planId와 기간(range)에 따른 장보기 리스트를 조회합니다.
 * @param {number} planId - 식단 플랜 ID
 * @param {'TODAY' | 'WEEK' | 'MONTH'} range - 조회 기간
 * @returns {Promise<object>} - 장보기 리스트 응답 데이터
 */
export async function fetchShoppingList(planId, range) {
  const res = await http.get(`/meal-plans/${planId}/shopping`, {
    params: { range },
  });
  return res.data ?? res;
}

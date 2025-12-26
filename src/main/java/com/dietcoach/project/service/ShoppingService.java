package com.dietcoach.project.service;

import com.dietcoach.project.dto.ShoppingProductsResponse;
import com.dietcoach.project.dto.meal.ShoppingListResponse;

public interface ShoppingService {

    // ✅ 기존 유지
    ShoppingProductsResponse search(String keyword, int page, int size);

    // ✅ A2 추가: 재료명(keyword)로 대표상품 1개만 뽑기
    SearchOneResult searchOne(String keyword, int allocatedBudget, Long totalGram);

    // ✅ 실구매 결과 기반 단가 추정 (원/100g)
    Integer getEstimatedCostPer100g(String ingredientName);

    // ✅ MealPlanServiceImpl에서 그대로 사용
    record SearchOneResult(ShoppingListResponse.ProductCard product, String source) {}
}

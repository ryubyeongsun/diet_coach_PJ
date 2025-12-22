package com.dietcoach.project.client.shopping;

public interface ShoppingClient {
    ShoppingClientResult searchProducts(String keyword, int page, int size);
    
    /**
     * ✅ 카테고리 타겟팅 검색 지원을 위한 오버로딩
     */
    default ShoppingClientResult searchProducts(String keyword, int page, int size, String dispCtgrNo) {
        return searchProducts(keyword, page, size);
    }
}

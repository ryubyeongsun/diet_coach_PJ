package com.dietcoach.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.client.shopping.ShoppingClient;
import com.dietcoach.project.client.shopping.ShoppingClientResult;
import com.dietcoach.project.domain.ShoppingProduct;
import com.dietcoach.project.dto.ShoppingProductResponse;
import com.dietcoach.project.dto.ShoppingProductsResponse;
import com.dietcoach.project.dto.meal.ShoppingListResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShoppingServiceImpl implements ShoppingService {

    private final ShoppingClient shoppingClient;

    @Override
    @Transactional(readOnly = true)
    public ShoppingProductsResponse search(String keyword, int page, int size) {
        ShoppingClientResult result = shoppingClient.searchProducts(keyword, page, size);

        List<ShoppingProductResponse> mapped = (result.getProducts() == null)
                ? List.of()
                : result.getProducts().stream().map(this::toResponse).toList();

        return ShoppingProductsResponse.builder()
                .source(result.getSource())
                .products(mapped)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchOneResult searchOne(String keyword) {
        ShoppingClientResult result = shoppingClient.searchProducts(keyword, 1, 1);

        String source = (result == null || result.getSource() == null) ? "REAL" : result.getSource();
        if (result == null || result.getProducts() == null || result.getProducts().isEmpty()) {
            return new SearchOneResult(null, source);
        }

        ShoppingProduct p = result.getProducts().get(0);

        ShoppingListResponse.ProductCard card = ShoppingListResponse.ProductCard.builder()
                .name(p.getTitle())
                .price(Long.valueOf(p.getPrice()))   // ✅ 여기 수정 (p.getPrice()가 int여도 OK)
                .imageUrl(p.getImageUrl())
                .detailUrl(p.getProductUrl())
                .build();

        return new SearchOneResult(card, source);
    }

    private ShoppingProductResponse toResponse(ShoppingProduct p) {
        return ShoppingProductResponse.builder()
                .externalId(p.getExternalId())
                .title(p.getTitle())
                .price(p.getPrice())
                .gramPerUnit(p.getGramPerUnit())
                .imageUrl(p.getImageUrl())
                .productUrl(p.getProductUrl())
                .mallName(p.getMallName())
                .build();
    }
}

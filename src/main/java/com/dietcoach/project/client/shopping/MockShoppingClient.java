package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MockShoppingClient implements ShoppingClient {

    @Override
    public ShoppingClientResult searchProducts(String keyword, int page, int size) {
        log.warn("[MockShoppingClient] returning MOCK products. keyword={}", keyword);

        List<ShoppingProduct> products = List.of(
                ShoppingProduct.builder()
                        .externalId("MOCK-001")
                        .title(keyword + " 샘플 상품 1")
                        .price(9900)
                        .gramPerUnit(null)
                        .imageUrl(null)
                        .productUrl(null)
                        .mallName("MOCK")
                        .build(),
                ShoppingProduct.builder()
                        .externalId("MOCK-002")
                        .title(keyword + " 샘플 상품 2")
                        .price(12900)
                        .gramPerUnit(null)
                        .imageUrl(null)
                        .productUrl(null)
                        .mallName("MOCK")
                        .build()
        );

        return ShoppingClientResult.builder()
                .products(products)
                .source("MOCK")
                .build();
    }
}

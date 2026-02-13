package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MockShoppingClient implements ShoppingClient {

    @Override
    public ShoppingClientResult searchProducts(String keyword, int page, int size) {
        List<ShoppingProduct> products = List.of(
                ShoppingProduct.builder()
                        .externalId("MOCK-001")
                        .title(keyword + " mock product 1")
                        .price(9900)
                        .gramPerUnit(null)
                        .imageUrl(null)
                        .productUrl(null)
                        .mallName("MOCK")
                        .build(),
                ShoppingProduct.builder()
                        .externalId("MOCK-002")
                        .title(keyword + " mock product 2")
                        .price(12900)
                        .gramPerUnit(null)
                        .imageUrl(null)
                        .productUrl(null)
                        .mallName("MOCK")
                        .build()
        );

        log.warn(
                "[SHOPPING_CLIENT][{}] MOCK keyword=\"{}\" responseCount={}",
                currentTraceId(),
                keyword,
                products.size()
        );

        return ShoppingClientResult.builder()
                .products(products)
                .source("MOCK")
                .build();
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? "no-trace" : traceId;
    }
}

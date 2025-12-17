package com.dietcoach.project.client.shopping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class HybridShoppingClient implements ShoppingClient {

    private final ElevenstShoppingClient elevenstShoppingClient;
    private final MockShoppingClient mockShoppingClient;

    @Value("${shopping.elevenst.useMockWhenError:true}")
    private boolean useMockWhenError;

    @Override
    public ShoppingClientResult searchProducts(String keyword, int page, int size) {
        try {
            ShoppingClientResult real = elevenstShoppingClient.searchProducts(keyword, page, size);

            if (real != null && real.getProducts() != null && !real.getProducts().isEmpty()) {
                return real; // source=REAL
            }

            log.warn("[HybridShoppingClient] Empty result from REAL API, fallback to MOCK");
            return mockShoppingClient.searchProducts(keyword, page, size);

        } catch (Exception e) {
            log.error("[HybridShoppingClient] REAL API failed", e);

            if (!useMockWhenError) throw e;

            log.warn("[HybridShoppingClient] Using MOCK fallback");
            return mockShoppingClient.searchProducts(keyword, page, size);
        }
    }
}

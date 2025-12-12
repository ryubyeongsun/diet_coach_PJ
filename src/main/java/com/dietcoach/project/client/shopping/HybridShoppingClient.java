package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public List<ShoppingProduct> searchProducts(String keyword, int page, int size) {
        try {
            List<ShoppingProduct> real =
                    elevenstShoppingClient.searchProducts(keyword, page, size);

            if (real != null && !real.isEmpty()) {
                return real;
            }

            log.warn("[HybridShoppingClient] Empty result from real API, fallback to mock");
            return mockShoppingClient.searchProducts(keyword, page, size);

        } catch (Exception e) {
            log.error("[HybridShoppingClient] Real API failed", e);

            if (!useMockWhenError) {
                throw e;
            }

            log.warn("[HybridShoppingClient] Using mock fallback");
            return mockShoppingClient.searchProducts(keyword, page, size);
        }
    }
}

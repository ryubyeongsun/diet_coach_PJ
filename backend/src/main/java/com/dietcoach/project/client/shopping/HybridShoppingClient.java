package com.dietcoach.project.client.shopping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

            log.warn("[SHOPPING_CLIENT][{}] REAL_EMPTY keyword=\"{}\" fallback=MOCK", currentTraceId(), keyword);
            return mockShoppingClient.searchProducts(keyword, page, size);

        } catch (Exception e) {
            log.error(
                    "[SHOPPING_CLIENT][{}] REAL_FAIL keyword=\"{}\" ex={} fallback={}",
                    currentTraceId(),
                    keyword,
                    e.getClass().getSimpleName(),
                    useMockWhenError ? "MOCK" : "NONE",
                    e
            );

            if (!useMockWhenError) throw e;

            return mockShoppingClient.searchProducts(keyword, page, size);
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? "no-trace" : traceId;
    }
}

package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MockShoppingClient {

    private final List<ShoppingProduct> mockProducts = new ArrayList<>();

    @PostConstruct
    void initMockData() {
        mockProducts.add(ShoppingProduct.builder()
                .externalId("mock-1001")
                .title("닭가슴살 1kg 대용량")
                .price(12900)
                .gramPerUnit(1000.0)
                .imageUrl("https://example.com/chicken-1kg.jpg")
                .productUrl("https://mock.shop/chicken")
                .mallName("MOCK")
                .build());

        mockProducts.add(ShoppingProduct.builder()
                .externalId("mock-2001")
                .title("현미 4kg 국산")
                .price(18900)
                .gramPerUnit(4000.0)
                .imageUrl("https://example.com/rice-4kg.jpg")
                .productUrl("https://mock.shop/rice")
                .mallName("MOCK")
                .build());

        log.info("[MockShoppingClient] initialized {} mock products", mockProducts.size());
    }

    public List<ShoppingProduct> searchProducts(String keyword, int page, int size) {
        List<ShoppingProduct> filtered = mockProducts.stream()
                .filter(p -> p.getTitle() != null && p.getTitle().contains(keyword))
                .collect(Collectors.toList());

        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(filtered.size(), fromIndex + size);

        if (fromIndex >= filtered.size()) {
            return List.of();
        }

        return filtered.subList(fromIndex, toIndex);
    }
}

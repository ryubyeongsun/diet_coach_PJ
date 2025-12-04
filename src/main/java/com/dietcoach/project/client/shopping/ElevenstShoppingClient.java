package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Temporary mock implementation for 11st shopping client.
 *
 * Later this class will be refactored to call 11st OPEN API via RestTemplate/WebClient.
 */
@Slf4j
@Component
public class ElevenstShoppingClient implements ShoppingClient {

    /**
     * In-memory mock dataset acting as a stand-in for real 11st responses.
     */
    private final List<ShoppingProduct> mockProducts = new ArrayList<>();

    @PostConstruct
    void initMockData() {
        // NOTE: sample data for development only.
        mockProducts.add(ShoppingProduct.builder()
                .externalId("11st-1001")
                .title("닭가슴살 1kg 대용량")
                .price(12900)
                .gramPerUnit(1000.0)
                .imageUrl("https://example.com/chicken-1kg.jpg")
                .productUrl("https://www.11st.co.kr/product/1001")
                .mallName("11st")
                .build());

        mockProducts.add(ShoppingProduct.builder()
                .externalId("11st-1002")
                .title("닭가슴살 500g x 2팩")
                .price(13900)
                .gramPerUnit(1000.0)
                .imageUrl("https://example.com/chicken-2pack.jpg")
                .productUrl("https://www.11st.co.kr/product/1002")
                .mallName("11st")
                .build());

        mockProducts.add(ShoppingProduct.builder()
                .externalId("11st-2001")
                .title("현미 4kg 국산")
                .price(18900)
                .gramPerUnit(4000.0)
                .imageUrl("https://example.com/brownrice-4kg.jpg")
                .productUrl("https://www.11st.co.kr/product/2001")
                .mallName("11st")
                .build());

        log.info("Initialized {} mock shopping products", mockProducts.size());
    }

    @Override
    public List<ShoppingProduct> searchProducts(String keyword, int page, int size) {
        // Simple keyword filter on title.
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

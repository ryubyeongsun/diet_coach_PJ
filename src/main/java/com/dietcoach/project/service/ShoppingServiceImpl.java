package com.dietcoach.project.service;

import com.dietcoach.project.client.shopping.ShoppingClient;
import com.dietcoach.project.domain.ShoppingProduct;
import com.dietcoach.project.dto.ShoppingProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Default implementation of ShoppingService using ShoppingClient abstraction.
 */
@Service
@RequiredArgsConstructor
public class ShoppingServiceImpl implements ShoppingService {

    private final ShoppingClient shoppingClient;

    @Override
    public List<ShoppingProductResponse> search(String keyword, Integer page, Integer size) {
        int resolvedPage = (page == null || page < 1) ? 1 : page;
        int resolvedSize = (size == null || size < 1) ? 10 : size;

        List<ShoppingProduct> products = shoppingClient.searchProducts(keyword, resolvedPage, resolvedSize);
        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ShoppingProductResponse> recommend(String ingredient, Integer neededGram) {
        // For now, simple heuristic:
        // - search products by ingredient keyword
        // - sort by pricePer100g (if gramPerUnit is available)
        // - return top 3 candidates
        int resolvedNeededGram = (neededGram == null || neededGram <= 0) ? 500 : neededGram;

        List<ShoppingProduct> candidates = shoppingClient.searchProducts(ingredient, 1, 20);

        return candidates.stream()
                .map(this::toResponse)
                .peek(resp -> resp.setPricePer100g(calculatePricePer100g(resp)))
                .sorted(Comparator.comparing(
                        // If pricePer100g is null, push it to the end.
                        resp -> resp.getPricePer100g() == null ? Double.MAX_VALUE : resp.getPricePer100g()
                ))
                .limit(3)
                .toList();
    }

    private ShoppingProductResponse toResponse(ShoppingProduct product) {
        Double pricePer100g = calculatePricePer100g(product);

        return ShoppingProductResponse.builder()
                .externalId(product.getExternalId())
                .name(product.getTitle())
                .price(product.getPrice())
                .gramPerUnit(product.getGramPerUnit())
                .pricePer100g(pricePer100g)
                .imageUrl(product.getImageUrl())
                .productUrl(product.getProductUrl())
                .mallName(product.getMallName())
                .build();
    }

    private Double calculatePricePer100g(ShoppingProduct product) {
        if (product.getGramPerUnit() == null || product.getGramPerUnit() <= 0) {
            return null;
        }
        double pricePerGram = product.getPrice() / product.getGramPerUnit();
        return Math.round(pricePerGram * 100 * 10.0) / 10.0; // one decimal place
    }

    private Double calculatePricePer100g(ShoppingProductResponse response) {
        if (response.getGramPerUnit() == null || response.getGramPerUnit() <= 0) {
            return null;
        }
        double pricePerGram = response.getPrice() / response.getGramPerUnit();
        return Math.round(pricePerGram * 100 * 10.0) / 10.0;
    }
}

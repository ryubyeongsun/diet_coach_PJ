package com.dietcoach.project.util.shopping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngredientQueryNormalizerTest {

    private final IngredientQueryNormalizer normalizer = new IngredientQueryNormalizer();

    @Test
    @DisplayName("Removes packaging tokens from ingredient query")
    void removesPackagingTokens() {
        assertEquals("토마토 신선식품", normalizer.normalize("토마토 박스"));
        assertEquals("양파", normalizer.normalize("양파 세트"));
    }
}

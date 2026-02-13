package com.dietcoach.project.util.shopping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductWeightParserTest {

    private final ProductWeightParser parser = new ProductWeightParser();

    @Test
    @DisplayName("Parses simple kg and g units")
    void parseSimpleUnits() {
        assertEquals(1000, parser.parseWeightInGrams("토마토 1kg"));
        assertEquals(500, parser.parseWeightInGrams("토마토 500g"));
    }

    @Test
    @DisplayName("Parses multiplication format like 500g x 2")
    void parseMultiplicationFormat() {
        assertEquals(1000, parser.parseWeightInGrams("토마토 500g x 2팩"));
        assertEquals(1500, parser.parseWeightInGrams("닭가슴살 500g*3"));
    }
}

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
    @Test
    @DisplayName("lb를 리터로 오인하지 않고 그램으로 변환한다")
    void parsePounds() {
        assertEquals(453, parser.parseWeightInGrams("아몬드 1lb"));
        assertEquals(907, parser.parseWeightInGrams("아몬드 2LB"));
    }

    @Test
    @DisplayName("기존 용량 단위와 소수점 해석을 유지한다")
    void preserveSupportedUnits() {
        assertEquals(1500, parser.parseWeightInGrams("쌀 1.5kg"));
        assertEquals(1500, parser.parseWeightInGrams("쌀 1,5kg"));
        assertEquals(1000, parser.parseWeightInGrams("우유 1L"));
        assertEquals(500, parser.parseWeightInGrams("우유 500ml"));
        assertEquals(28, parser.parseWeightInGrams("견과류 1oz"));
    }

    @Test
    @DisplayName("묶음 용량과 전체 용량 중 큰 값을 사용한다")
    void preferPackageTotal() {
        assertEquals(1000, parser.parseWeightInGrams("닭가슴살 100g x 10 (1kg)"));
    }

    @Test
    @DisplayName("용량이 없는 상품명은 0을 반환한다")
    void returnZeroForMissingWeight() {
        assertEquals(0, parser.parseWeightInGrams(null));
        assertEquals(0, parser.parseWeightInGrams(" "));
        assertEquals(0, parser.parseWeightInGrams("토마토"));
    }
}

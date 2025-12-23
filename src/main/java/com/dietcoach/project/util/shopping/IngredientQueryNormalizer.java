package com.dietcoach.project.util.shopping;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class IngredientQueryNormalizer {

    private static final Pattern UNIT_PATTERN = Pattern.compile("(?i)(\\d+(\\.\\d+)?)\\s*(kg|g|ml|l|lb|oz|ea|cm|mm|개|통|봉|팩|입|인분)");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    // Remove these tokens from query string
    private static final Set<String> REMOVE_TOKENS = Set.of(
            "냉동", "냉장", "국산", "국내산", "수입", "친환경", "유기농", "무농약",
            "세척", "손질", "슬라이스", "다진", "자른", "통", "껍질", "벗긴"
    );

    // Synonym map for better search results
    private static final Map<String, String> SYNONYM_MAP = new HashMap<>();

    static {
        SYNONYM_MAP.put("닭가슴살", "닭가슴살");
        SYNONYM_MAP.put("방울토마토", "방울토마토");
        SYNONYM_MAP.put("계란", "계란");
        SYNONYM_MAP.put("달걀", "계란");
        SYNONYM_MAP.put("고구마", "고구마");
        SYNONYM_MAP.put("감자", "감자");
        SYNONYM_MAP.put("양파", "양파");
        SYNONYM_MAP.put("마늘", "마늘");
        // Add more common ingredients if needed
    }

    public String normalize(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) {
            return "";
        }

        String normalized = ingredientName.trim();

        // 1. Remove units (e.g., 500g, 1kg)
        normalized = UNIT_PATTERN.matcher(normalized).replaceAll(" ");

        // 2. Remove special characters
        normalized = SPECIAL_CHAR_PATTERN.matcher(normalized).replaceAll(" ");

        // 3. Tokenize and filter
        String[] tokens = MULTI_SPACE_PATTERN.split(normalized);
        List<String> validTokens = new ArrayList<>();
        
        for (String token : tokens) {
            if (token.isBlank()) continue;
            if (REMOVE_TOKENS.contains(token)) continue;
            validTokens.add(token);
        }

        if (validTokens.isEmpty()) {
            // If everything is filtered out, revert to original (cleaned) or just "식품"
            // But usually we want to keep at least the main noun.
            // If the original was just "냉동 닭가슴살", "닭가슴살" remains.
            // If the original was just "국산", it becomes empty.
            return ingredientName.trim(); 
        }

        // 4. Join and apply synonyms
        String joined = String.join(" ", validTokens);
        
        // Check exact match synonym
        if (SYNONYM_MAP.containsKey(joined)) {
            return SYNONYM_MAP.get(joined);
        }

        return joined;
    }
}

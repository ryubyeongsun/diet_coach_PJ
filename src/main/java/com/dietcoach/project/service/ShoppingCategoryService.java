package com.dietcoach.project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Validates if a product's category is allowed based on a whitelist.
 * Also provides logic to find the best matching category for an ingredient.
 */
@Service
@Slf4j
public class ShoppingCategoryService {

    private final Set<String> ALLOWED_CODES = new HashSet<>();
    private final Map<String, String> KEYWORD_TO_CAT_MAP = new HashMap<>();
    
    private static final List<String> BANNED_CATEGORY_KEYWORDS = List.of(
        "선물세트", "반려동물", "강아지", "고양이", "펫", "사료", 
        "다이어트", "보조식품", "건강기능", 
        "주류", "무알콜", "비알콜", 
        "가구", "인테리어", "생활용품", "주방용품", "그릇", "냄비", "조리도구",
        "장난감", "문구", "도서", "음반", "DVD",
        "가전", "디지털", "컴퓨터", "휴대폰",
        "무드등", "램프", "조명", 
        "틀", "몰드", "에그팬", "주방",
        "니트", "원피스", "셔츠", "바지", "팬츠", "의류", "티셔츠", // 의류 추가
        "커피머신", "제조기", "믹서기", // 가전 추가
        "계란틀", "요리틀", "모양틀", "에그몰드", "GF" // 특정 노이즈 상품 추가
    );

    public ShoppingCategoryService() {
        // 1. Root & Depth 2 (기본 화이트리스트)
        ALLOWED_CODES.addAll(List.of("1001335", "1001336", "1001338", "1001341"));
        ALLOWED_CODES.addAll(List.of("1001470", "1001471", "1001472", "1001473"));
        ALLOWED_CODES.addAll(List.of("1001480", "1001481", "1001482", "1001483", "1001484", "1001486", "1001487", "1001488", "1129543"));
        ALLOWED_CODES.addAll(List.of("1002081", "1002085", "1002087", "1002088", "1002089", "1002090", "1002091", "1002092", "1002093", "1002094", "1002095", "1129367", "1129369", "1129384", "1340349", "1340350"));
        ALLOWED_CODES.addAll(List.of("1001475", "1001476", "1001477", "1129438", "1129444", "1129447", "1129450", "1129455", "1129460", "1129469", "1129471", "1129479", "1348299", "1348303"));
        
        // 2. ✅ 혁신: 재료 키워드별 타겟 카테고리 매핑 (Targeting Map)
        // 축산
        KEYWORD_TO_CAT_MAP.put("계란", "1129543");
        KEYWORD_TO_CAT_MAP.put("달걀", "1129543");
        KEYWORD_TO_CAT_MAP.put("닭가슴살", "1009255");
        KEYWORD_TO_CAT_MAP.put("돼지", "1001482");
        KEYWORD_TO_CAT_MAP.put("소고기", "1001480");
        KEYWORD_TO_CAT_MAP.put("쇠고기", "1001480");
        // 수산
        KEYWORD_TO_CAT_MAP.put("김", "1007765");
        KEYWORD_TO_CAT_MAP.put("미역", "1007768");
        KEYWORD_TO_CAT_MAP.put("다시마", "1007766");
        KEYWORD_TO_CAT_MAP.put("연어", "1001471");
        // 가공/냉동
        KEYWORD_TO_CAT_MAP.put("소금", "1002090");
        KEYWORD_TO_CAT_MAP.put("간장", "1340679");
        KEYWORD_TO_CAT_MAP.put("올리브유", "1341017");
        KEYWORD_TO_CAT_MAP.put("만두", "1129450");
        KEYWORD_TO_CAT_MAP.put("샐러드", "1129455");
        KEYWORD_TO_CAT_MAP.put("요거트", "1001475"); // 기타 식품/유제품
        
        ALLOWED_CODES.addAll(KEYWORD_TO_CAT_MAP.values());
        log.info("Initialized ShoppingCategoryService with {} targeting rules.", KEYWORD_TO_CAT_MAP.size());
    }

    /**
     * ✅ 혁신: 재료명에 가장 적합한 카테고리 코드를 찾습니다. (사전 타겟팅용)
     */
    public Optional<String> findBestCategory(String ingredientName) {
        if (ingredientName == null) return Optional.empty();
        
        for (Map.Entry<String, String> entry : KEYWORD_TO_CAT_MAP.entrySet()) {
            if (ingredientName.contains(entry.getKey())) {
                log.info("[CATEGORY_TARGET] Found target category for \"{}\" -> {}", ingredientName, entry.getValue());
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public boolean isValidCategory(String dispNo, String dispNm) {
        if (dispNm != null && !dispNm.isBlank()) {
            for (String banned : BANNED_CATEGORY_KEYWORDS) {
                if (dispNm.contains(banned)) return false;
            }
        }
        if (dispNo != null && !dispNo.isBlank()) {
            return ALLOWED_CODES.contains(dispNo);
        }
        return true; 
    }

    /**
     * ✅ 캐시 검증용: 상품명에 금지 키워드가 있는지 확인
     */
    public boolean isValidProductTitle(String title) {
        if (title == null || title.isBlank()) return false;
        for (String banned : BANNED_CATEGORY_KEYWORDS) {
            if (title.contains(banned)) return false;
        }
        return true;
    }
}
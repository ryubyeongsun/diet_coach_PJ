package com.dietcoach.project.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.client.ai.DietAiClient;
import com.dietcoach.project.client.ai.dto.AiMonthlySkeletonResponse;
import com.dietcoach.project.common.TdeeCalculator;
import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.WeightRecord;
import com.dietcoach.project.domain.meal.MealIntake;
import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealDetailResponse;
import com.dietcoach.project.dto.meal.MealItemResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.dto.meal.ShoppingListResponse;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.WeightRecordMapper;
import com.dietcoach.project.mapper.meal.MealIntakeMapper;
import com.dietcoach.project.mapper.meal.MealPlanMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanServiceImpl implements MealPlanService {

    private static final int DEFAULT_PLAN_DAYS = 30;
    private static final int AI_CHUNK_DAYS = 7;
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    // A2 캐시 TTL 1시간
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(1);
    private static final int SHOPPING_SEARCH_TOP_N = 5;

    private final UserMapper userMapper;
    private final MealPlanMapper mealPlanMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final MealIntakeMapper mealIntakeMapper;

    // AI Client
    private final DietAiClient dietAiClient;

    // 11번가 대표상품 1개 조회 서비스
    private final ShoppingService shoppingService;
    private final ShoppingCategoryService categoryService;

    // =========================
    // A2 캐시: ingredientName(lower) -> CachedProduct
    // =========================
    private final Map<String, CachedProduct> productCache = new HashMap<>();

    private static class CachedProduct {
        final ShoppingListResponse.ProductCard product;
        final String source; // REAL|MOCK|NONE
        final long expireAtMillis;

        CachedProduct(ShoppingListResponse.ProductCard product, String source, long expireAtMillis) {
            this.product = product;
            this.source = source;
            this.expireAtMillis = expireAtMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAtMillis;
        }
    }

    // =========================
    // TEMPLATE 카탈로그/템플릿
    // =========================
    @Getter
    @AllArgsConstructor
    private static class FoodItem {
        private final String name;
        private final int caloriesPer100g;
    }

    @Getter
    @AllArgsConstructor
    private static class FoodPortion {
        private final FoodItem food;
        private final int baseGrams;
    }

    private static final List<FoodPortion> BREAKFAST_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("오트밀", 380), 40),
            new FoodPortion(new FoodItem("그릭요거트", 60), 150)
    );

    private static final List<FoodPortion> LUNCH_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("밥", 150), 200),
            new FoodPortion(new FoodItem("닭가슴살", 165), 150),
            new FoodPortion(new FoodItem("샐러드", 40), 80)
    );

    private static final List<FoodPortion> DINNER_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("밥", 150), 150),
            new FoodPortion(new FoodItem("연어", 200), 120),
            new FoodPortion(new FoodItem("샐러드", 40), 80)
    );

    @Override
    @Transactional
    public MealPlanOverviewResponse createMonthlyPlan(Long userId, MealPlanCreateRequest request) {
        LocalDate startDate = (request != null && request.getStartDate() != null)
                ? request.getStartDate()
                : LocalDate.now();

        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException("존재하지 않는 사용자입니다. id=" + userId);

        if (user.getBmr() == null || user.getTdee() == null || user.getTargetCalories() == null) {
            TdeeCalculator.fillUserEnergyFields(user);
            userMapper.updateUserEnergy(user);
        }

        int targetKcalPerDay = (int) Math.round(user.getTargetCalories());
        LocalDate endDate = startDate.plusDays(DEFAULT_PLAN_DAYS - 1);

        int mealsPerDay = (request != null && request.getMealsPerDay() != null) ? request.getMealsPerDay() : 3;
        if (mealsPerDay < 1 || mealsPerDay > 3) throw new BusinessException("mealsPerDay는 1~3만 가능합니다.");

        log.info("[MealPlan] createMonthlyPlan start userId={}, startDate={}, mealsPerDay={}",
                userId, startDate, mealsPerDay);

        // 1. Create Plan & Days Skeleton (Empty)
        MealPlan mealPlan = MealPlan.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(DEFAULT_PLAN_DAYS)
                .targetCaloriesPerDay(targetKcalPerDay)
                .monthlyBudget(request != null ? request.getMonthlyBudget() : null)
                .mealsPerDay(mealsPerDay)
                .preferences(toCsvSafe(request != null ? request.getPreferences() : null))
                .allergies(toCsvSafe(request != null ? request.getAllergies() : null))
                .build();

        mealPlanMapper.insertMealPlan(mealPlan);

        List<MealPlanDay> allDays = new ArrayList<>();
        for (int i = 0; i < DEFAULT_PLAN_DAYS; i++) {
            MealPlanDay day = MealPlanDay.builder()
                    .mealPlanId(mealPlan.getId())
                    .planDate(startDate.plusDays(i))
                    .dayIndex(i + 1)
                    .totalCalories(0)
                    .build();
            mealPlanMapper.insertMealPlanDay(day);
            allDays.add(day);
        }

        // Base Payload for AI
        Map<String, Object> basePayload = new HashMap<>();
        basePayload.put("targetCaloriesPerDay", targetKcalPerDay);
        basePayload.put("mealsPerDay", mealsPerDay);
        basePayload.put("monthlyBudget", request != null ? request.getMonthlyBudget() : null);
        basePayload.put("preferences", request != null ? request.getPreferences() : List.of());
        basePayload.put("allergies", request != null ? request.getAllergies() : List.of());
        basePayload.put("goalType", user.getGoalType() != null ? user.getGoalType().name() : "MAINTAIN");

        // 2. Generate First Week (Sync) - Days 0-6
        log.info("[MealPlan] Generating Week 1 Sync...");
        generateAndSaveChunk(mealPlan, allDays, basePayload, 0, 7, targetKcalPerDay, mealsPerDay);

        // 3. Generate Remaining Weeks (Async) - Days 7-29
        // We use CompletableFuture to run this in background without blocking response
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                generateRemainingWeeksParallel(mealPlan, allDays, basePayload, targetKcalPerDay, mealsPerDay);
            } catch (Exception e) {
                log.error("[MealPlan] Async generation failed for planId={}", mealPlan.getId(), e);
            }
        });

        // 4. Return Response (Contains populated Week 1 and empty subsequent weeks)
        return getMealPlan(mealPlan.getId());
    }

    private void generateRemainingWeeksParallel(
            MealPlan mealPlan,
            List<MealPlanDay> allDays,
            Map<String, Object> basePayload,
            int targetKcal,
            int mealsPerDay
    ) {
        log.info("[MealPlan] Starting Async Parallel Generation for remaining weeks...");
        
        // Define chunks for remaining days (e.g. 7-13, 14-20, 21-29)
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        
        // From day 7 to 29
        for (int offset = 7; offset < DEFAULT_PLAN_DAYS; offset += AI_CHUNK_DAYS) {
            int currentOffset = offset;
            int limit = Math.min(AI_CHUNK_DAYS, DEFAULT_PLAN_DAYS - currentOffset);
            
            // Create a parallel task for each chunk
            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                log.info("[MealPlan] Parallel Chunk Start offset={}", currentOffset);
                generateAndSaveChunk(mealPlan, allDays, basePayload, currentOffset, limit, targetKcal, mealsPerDay);
            }));
        }

        // Wait for all parallel chunks to complete
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        log.info("[MealPlan] Async Parallel Generation Completed for planId={}", mealPlan.getId());
    }

    private void generateAndSaveChunk(
            MealPlan mealPlan,
            List<MealPlanDay> allDays,
            Map<String, Object> basePayload,
            int offset,
            int limit,
            int targetKcal,
            int mealsPerDay
    ) {
        LocalDate chunkStart = mealPlan.getStartDate().plusDays(offset);
        
        Map<String, Object> payload = new HashMap<>(basePayload);
        payload.put("startDate", chunkStart.format(DF));
        payload.put("totalDays", limit);

        AiMonthlySkeletonResponse skeleton = null;
        try {
            skeleton = dietAiClient.generateMonthlySkeleton(payload);
        } catch (Exception e) {
            log.warn("[MealPlan] AI failed for chunk offset={}, reason={}", offset, e.getMessage());
        }

        // Calculate Meal Targets
        List<String> mealTimesForDay = switch (mealsPerDay) {
            case 1 -> List.of("LUNCH");
            case 2 -> List.of("BREAKFAST", "DINNER");
            default -> List.of("BREAKFAST", "LUNCH", "DINNER");
        };
        Map<String, Integer> mealTargets = distributeCalories(targetKcal, mealTimesForDay);

        // Save Items
        for (int i = 0; i < limit; i++) {
            int dayIndex = offset + i; // 0-based global index
            if (dayIndex >= allDays.size()) break;

            MealPlanDay day = allDays.get(dayIndex);
            List<MealItem> itemsForDay;

            if (skeleton != null) {
                // Determine logic day index within skeleton (0 to limit-1)
                itemsForDay = buildItemsFromAiWithFixedRules(skeleton, i, day.getId(), mealsPerDay, mealTargets);
            } else {
                itemsForDay = buildItemsFromTemplate(day.getId(), targetKcal, mealsPerDay);
            }

            // Using synchronized block or transaction might be needed if strictly enforcing transactional integrity,
            // but for bulk insert in separate logic, calling mapper directly is fine.
            // Note: Since we are in a async thread, we don't have the parent transaction.
            // Ideally we should wrap this in a transaction or save safely.
            saveItemsForDay(day, itemsForDay);
        }
    }

    private void saveItemsForDay(MealPlanDay day, List<MealItem> items) {
        for (MealItem item : items) {
            mealPlanMapper.insertMealItem(item);
        }
        int totalCaloriesForDay = items.stream().mapToInt(MealItem::getCalories).sum();
        day.setTotalCalories(totalCaloriesForDay);
        mealPlanMapper.updateMealPlanDayTotalCalories(day.getId(), totalCaloriesForDay);
    }

    // Removing old generateAiSkeletonInChunks as it's replaced by generateRemainingWeeksParallel
    /*
    private AiMonthlySkeletonResponse generateAiSkeletonInChunks(...) { ... }
    */

    private List<MealItem> buildItemsFromAiWithFixedRules(
            AiMonthlySkeletonResponse skeleton,
            int dayOffset,
            Long mealPlanDayId,
            int mealsPerDay,
            Map<String, Integer> mealTargets
    ) {
        if (skeleton == null || skeleton.getDays() == null || skeleton.getDays().isEmpty()) return List.of();
        if (skeleton.getDays().size() <= dayOffset) return List.of();

        AiMonthlySkeletonResponse.AiDaySkeleton daySk = skeleton.getDays().get(dayOffset);
        if (daySk == null || daySk.getMeals() == null) return List.of();

        Set<String> allowedMealTimes = switch (mealsPerDay) {
            case 1 -> Set.of("LUNCH");
            case 2 -> Set.of("BREAKFAST", "DINNER");
            default -> Set.of("BREAKFAST", "LUNCH", "DINNER");
        };

        List<MealItem> result = new ArrayList<>();

        for (AiMonthlySkeletonResponse.AiMealSkeleton meal : daySk.getMeals()) {
            String mt = normalizeMealTime(meal.getMealTime());
            if (mt == null) continue;
            if (!allowedMealTimes.contains(mt)) continue;

            String menuName = safeTrim(meal.getMenuName());

            List<AiMonthlySkeletonResponse.AiIngredient> ings =
                    (meal.getIngredients() == null) ? List.of() : meal.getIngredients();

            for (AiMonthlySkeletonResponse.AiIngredient ing : ings) {
                if (ing == null) continue;
                String ingredient = safeTrim(ing.getIngredientName());
                if (ingredient.isEmpty()) continue;

                // 1) grams 확정
                Integer grams = adjustGrams(ing.getGrams(), ingredient);

                String memo = menuName.isEmpty() ? "AI" : menuName;

                // 2) 객체 생성 (일단 칼로리 0으로 생성)
                MealItem item = MealItem.builder()
                        .mealPlanDayId(mealPlanDayId)
                        .mealTime(mt)
                        .foodName(ingredient)
                        .grams(grams)
                        .calories(0)
                        .memo(memo)
                        .build();
                
                // 3) 정밀 영양 분석 엔진 돌리기 (Calories, Carbs, Protein, Fat 계산)
                calculateAndSetNutrients(item);

                // 중복 허용: AI가 보낸 그대로 추가
                result.add(item);
            }
        }

        // =========================================================
        // 3) Post-Scaling: 총 칼로리가 목표 대비 너무 낮거나 높으면 전체적으로 조정
        // =========================================================
        if (mealTargets != null) {
            int targetTotal = mealTargets.values().stream().mapToInt(Integer::intValue).sum();
            int currentTotal = result.stream().mapToInt(MealItem::getCalories).sum();
            
            // 오차 범위 10% 초과 시 양방향 스케일링 수행
            if (targetTotal > 0 && currentTotal > 0 && 
               (currentTotal < targetTotal * 0.9 || currentTotal > targetTotal * 1.1)) {
                
                double scale = (double) targetTotal / currentTotal;
                // 안전 장치: 최대 1.5배 증량, 최소 0.5배 감량
                scale = Math.max(0.5, Math.min(1.5, scale));
                
                log.info("[SCALING_APPLIED] target={} current={} scale={}", targetTotal, currentTotal, String.format("%.2f", scale));
                
                List<MealItem> scaledItems = new ArrayList<>();
                for (MealItem item : result) {
                    int newGrams = (int) Math.round(item.getGrams() * scale);
                    // Re-calculate nutrients based on new grams
                    item.setGrams(newGrams);
                    calculateAndSetNutrients(item);
                    scaledItems.add(item);
                }
                return scaledItems;
            }
        }

        return result;
    }

    private void validateAiSkeleton(AiMonthlySkeletonResponse skeleton) {
        if (skeleton == null || skeleton.getDays() == null || skeleton.getDays().isEmpty()) {
            throw new BusinessException("AI skeleton empty");
        }

        for (AiMonthlySkeletonResponse.AiDaySkeleton day : skeleton.getDays()) {
            if (day == null || day.getDayIndex() == null || day.getPlanDate() == null || day.getMeals() == null) {
                throw new BusinessException("AI skeleton missing required day fields");
            }
            for (AiMonthlySkeletonResponse.AiMealSkeleton meal : day.getMeals()) {
                if (meal == null || normalizeMealTime(meal.getMealTime()) == null) {
                    throw new BusinessException("AI skeleton has invalid mealTime");
                }
            }
        }
    }

    private void logAiResponseSummary(AiMonthlySkeletonResponse skeleton) {
        int days = (skeleton.getDays() == null ? 0 : skeleton.getDays().size());
        int ingredientCount = 0;
        int caloriesMissing = 0;
        int gramsMissing = 0;

        if (skeleton.getDays() != null) {
            for (AiMonthlySkeletonResponse.AiDaySkeleton day : skeleton.getDays()) {
                if (day == null || day.getMeals() == null) continue;
                for (AiMonthlySkeletonResponse.AiMealSkeleton meal : day.getMeals()) {
                    if (meal == null || meal.getIngredients() == null) continue;
                    for (AiMonthlySkeletonResponse.AiIngredient ing : meal.getIngredients()) {
                        if (ing == null) continue;
                        ingredientCount++;
                        if (ing.getCalories() == null) caloriesMissing++;
                        if (ing.getGrams() == null) gramsMissing++;
                    }
                }
            }
        }

        log.info("[AI] response summary days={}, ingredients={}, caloriesMissing={}, gramsMissing={}",
                days, ingredientCount, caloriesMissing, gramsMissing);
    }

    // =========================
    // 정밀 영양 분석 엔진
    // =========================

    private Integer adjustGrams(Integer grams, String ingredientName) {
        // 1) AI grams 무조건 신뢰
        if (grams != null && grams > 0) {
            return grams;
        }
        // 2) 없으면 기본값 (추후 고도화 가능)
        return 100;
    }

    private void calculateAndSetNutrients(MealItem item) {
        // 공백을 제거하여 "기름 뺀" -> "기름뺀"으로 인식하게 함
        String n = normalizeName(item.getFoodName()).replace(" ", "");
        int grams = item.getGrams();
        
        double kpg = 1.2;
        double cRatio = 0.4; double pRatio = 0.3; double fRatio = 0.3;

        // 1. 예외 처리 강화 (공백 제거 상태로 체크)
        boolean isFatRemoved = n.contains("기름뺀") || n.contains("기름제거") || n.contains("지방제거") || n.contains("저지방") || n.contains("언스위트");

        // 2. 우선순위 재배치: 음료/대체유를 최우선으로 (Tier 0)
        if (containsAny(n, "아몬드브리즈", "아몬드유", "almondbreeze", "almondmilk", "우유", "두유", "음료")) {
            kpg = 0.3; cRatio = 0.5; pRatio = 0.2; fRatio = 0.3;
        }
        // 3. 저지방 단백질 (참치 기름 뺀 것 등은 여기서 먼저 걸러짐)
        else if (isFatRemoved || containsAny(n, "닭가슴살", "흰살생선", "새우", "오징어", "참치", "계란", "달걀", "명태", "동태")) {
            kpg = 1.2; cRatio = 0.05; pRatio = 0.85; fRatio = 0.10;
        }
        // 4. 순수 유지류 (isFatRemoved가 아님이 보장됨)
        else if (containsAny(n, "oil", "butter", "기름", "오일", "버터", "마요네즈", "참기름", "들기름", "식용유")) {
            kpg = 8.5; cRatio = 0.0; pRatio = 0.0; fRatio = 1.0;
        }
        // 5. 조리된 면/빵류 (상수 하향: 2.5 -> 1.5)
        else if (containsAny(n, "면", "국수", "파스타", "라면", "칼국수", "우동", "잔치국수", "빵", "베이글", "떡")) {
            kpg = 1.5; cRatio = 0.80; pRatio = 0.12; fRatio = 0.08;
        }
        // [Tier 2] 견과류
        else if (containsAny(n, "nuts", "almond", "peanut", "walnut", "견과", "아몬드", "땅콩", "호두")) {
            kpg = 6.0; cRatio = 0.15; pRatio = 0.15; fRatio = 0.70;
        }
        // [Tier 3] 고지방 단백질
        else if (containsAny(n, "pork", "belly", "beef", "mackerel", "salmon", "돼지", "삼겹살", "소고기", "쇠고기", "고등어", "연어", "갈비")) {
            kpg = 2.5; cRatio = 0.05; pRatio = 0.40; fRatio = 0.55;
        }
        // [Tier 5] 밥/구황작물
        else if (containsAny(n, "밥", "현미", "고구마", "감자", "옥수수", "rice", "potato")) {
            kpg = 1.4; cRatio = 0.88; pRatio = 0.08; fRatio = 0.04;
        }
        // [Tier 8] 양념
        else if (containsAny(n, "sauce", "장", "소스", "양념", "케찹", "마요")) {
            kpg = 1.6; cRatio = 0.6; pRatio = 0.2; fRatio = 0.2;
        }
        // [Tier 9] 채소
        else if (containsAny(n, "샐러드", "김치", "배추", "숙주", "나물", "채소", "상추", "깻잎", "오이", "당근", "버섯", "브로콜리")) {
            kpg = 0.35; cRatio = 0.70; pRatio = 0.20; fRatio = 0.10;
        }

        // 최종 산출
        int totalKcal = (int) Math.round(grams * kpg);
        item.setCalories(totalKcal);
        item.setCarbs((int) Math.round((totalKcal * cRatio) / 4.0));
        item.setProtein((int) Math.round((totalKcal * pRatio) / 4.0));
        item.setFat((int) Math.round((totalKcal * fRatio) / 9.0));
        item.setIsHighProtein((item.getProtein() * 4.0) >= (totalKcal * 0.3));
    }

    private String normalizeName(String ingredientName) {
        if (ingredientName == null) return "";
        return ingredientName.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null) return false;
        String v = value.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (token == null) continue;
            if (v.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private String normalizeMealTime(String v) {
        if (v == null) return null;
        String t = v.trim().toUpperCase();
        return switch (t) {
            case "BREAKFAST", "LUNCH", "DINNER", "SNACK" -> t;
            case "BRUNCH" -> "LUNCH";
            default -> null;
        };
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    // =========================
    // TEMPLATE 생성
    // =========================

    private List<MealItem> buildItemsFromTemplate(Long mealPlanDayId, int targetKcalPerDay, int mealsPerDay) {
        List<String> mealTimes = switch (mealsPerDay) {
            case 1 -> List.of("LUNCH");
            case 2 -> List.of("BREAKFAST", "DINNER");
            default -> List.of("BREAKFAST", "LUNCH", "DINNER");
        };

        Map<String, Integer> targets = distributeCalories(targetKcalPerDay, mealTimes);

        List<MealItem> items = new ArrayList<>();
        for (String mt : mealTimes) {
            items.addAll(generateTemplateMealItems(mealPlanDayId, mt, targets.get(mt)));
        }
        return items;
    }

    private List<MealItem> generateTemplateMealItems(Long mealPlanDayId, String mealTime, int targetCaloriesForMeal) {
        List<FoodPortion> template = switch (mealTime) {
            case "BREAKFAST" -> BREAKFAST_TEMPLATE;
            case "LUNCH" -> LUNCH_TEMPLATE;
            case "DINNER" -> DINNER_TEMPLATE;
            default -> BREAKFAST_TEMPLATE;
        };

        int baseTotalCalories = template.stream()
                .mapToInt(p -> p.getBaseGrams() * p.getFood().getCaloriesPer100g() / 100)
                .sum();

        double scale = baseTotalCalories > 0 ? (double) targetCaloriesForMeal / baseTotalCalories : 1.0;

        List<MealItem> result = new ArrayList<>();
        for (FoodPortion portion : template) {
            int scaledGrams = Math.max(1, (int) Math.round(portion.getBaseGrams() * scale));
            int itemCalories = scaledGrams * portion.getFood().getCaloriesPer100g() / 100;

            result.add(MealItem.builder()
                    .mealPlanDayId(mealPlanDayId)
                    .mealTime(mealTime)
                    .foodName(portion.getFood().getName())
                    .grams(scaledGrams)
                    .calories(itemCalories)
                    .memo("TEMPLATE")
                    .build());
        }
        return result;
    }

    private String toCsvSafe(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return list.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    private Map<String, Integer> distributeCalories(int total, List<String> mealTimes) {
        Map<String, Integer> map = new HashMap<>();

        if (mealTimes.size() == 1) {
            map.put(mealTimes.get(0), total);
            return map;
        }

        if (mealTimes.size() == 2) {
            int b = (int) Math.round(total * 0.45);
            map.put("BREAKFAST", b);
            map.put("DINNER", total - b);
            return map;
        }

        int b = (int) Math.round(total * 0.30);
        int l = (int) Math.round(total * 0.40);
        int d = total - b - l;
        map.put("BREAKFAST", b);
        map.put("LUNCH", l);
        map.put("DINNER", d);
        return map;
    }

    // =========================
    // A2) 장보기 리스트 + 대표상품 매핑(완료)
    // =========================

    @Override
    @Transactional(readOnly = true)
    public ShoppingListResponse getShoppingList(Long planId, String range) {
        MealPlan plan = mealPlanMapper.findMealPlanById(planId);
        if (plan == null) throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);

        RangeWindow w = resolveRange(plan, range);

        boolean useRange = range != null && !range.isBlank() && !"MONTH".equalsIgnoreCase(range);
        List<MealPlanIngredientResponse> ingredients = useRange
                ? mealPlanMapper.findIngredientsForPlanInRange(planId, w.from, w.to)
                : mealPlanMapper.findIngredientsForPlan(planId);

        String traceId = currentTraceId();
        log.info("[SHOPPING_LIST][{}] INGREDIENT_SUMMARY top10={}", traceId, buildIngredientSummary(ingredients));

        // A2-R4 Budget Allocation
        int planMonthlyBudget = (plan.getMonthlyBudget() != null && plan.getMonthlyBudget() > 0) 
                ? plan.getMonthlyBudget().intValue() : 300000;
        
        double rangeFraction = 1.0;
        if ("WEEK".equalsIgnoreCase(range)) rangeFraction = 7.0 / 30.0;
        else if ("TODAY".equalsIgnoreCase(range)) rangeFraction = 1.0 / 30.0;
        
        int rangeBudget = (int) (planMonthlyBudget * rangeFraction);

        // Calculate Weights & Total
        double totalWeight = 0.0;
        Map<String, Double> weightMap = new HashMap<>();
        for (MealPlanIngredientResponse ing : ingredients) {
             String name = safeTrim(ing.getIngredientName());
             if (name.isEmpty()) continue;
             
             double weight = 1.0;
             if (ing.getDaysCount() != null && ing.getDaysCount() > 0) {
                 weight = ing.getDaysCount() * 10.0; // Day count has high priority
             } else if (toLongSafe(ing.getTotalGram()) != null) {
                 weight = toLongSafe(ing.getTotalGram()) / 100.0; // 100g = 1 point
             }
             weightMap.put(name, weight);
             totalWeight += weight;
        }
        if (totalWeight <= 0) totalWeight = 1.0;

        List<ShoppingListResponse.ShoppingItem> items = new ArrayList<>();

        for (MealPlanIngredientResponse ing : ingredients) {
            String ingredientName = safeTrim(ing.getIngredientName());
            if (ingredientName.isEmpty()) continue;

            Long totalGram = toLongSafe(ing.getTotalGram());
            if (totalGram == null || totalGram <= 0) continue;

            Integer totalCalories = toIntSafe(ing.getTotalCalories());
            
            // Calculate Allocated Budget
            double weight = weightMap.getOrDefault(ingredientName, 1.0);
            int allocated = (int) (rangeBudget * (weight / totalWeight));
            
            // Clamp (Min 2k, Max 30k)
            if (allocated < 2000) allocated = 2000;
            if (allocated > 30000) allocated = 30000;

            ProductLookup lookup = getRepresentativeProductCached(ingredientName, allocated);

            items.add(ShoppingListResponse.ShoppingItem.builder()
                    .ingredientName(ingredientName)
                    .totalGram(totalGram)
                    .totalCalories(totalCalories)
                    .daysCount(ing.getDaysCount())
                    .product(lookup.product)
                    .source(lookup.source)
                    .build());
        }

        String overallSource = items.stream().anyMatch(i -> "REAL".equals(i.getSource())) ? "REAL" : "MOCK";
        String normalizedRange = (range == null || range.isBlank()) ? "MONTH" : range.trim().toUpperCase();

        return ShoppingListResponse.builder()
                .planId(planId)
                .range(normalizedRange)
                .startDate(w.from)
                .endDate(w.to)
                .budget((long) rangeBudget) // ✅ Set the calculated range budget
                .source(overallSource)
                .items(items)
                .build();
    }

    private static class ProductLookup {
        final ShoppingListResponse.ProductCard product;
        final String source;
        ProductLookup(ShoppingListResponse.ProductCard product, String source) {
            this.product = product;
            this.source = source;
        }
    }

    private ProductLookup getRepresentativeProductCached(String ingredientName, int allocatedBudget) {
        String traceId = currentTraceId();
        
        // A2-R5 Cache Key Improvement: ingredient + priceBucket
        // Bucket size: 2000 KRW
        int bucket = (allocatedBudget / 2000) * 2000;
        String normalizedName = normalizeCacheKey(ingredientName);
        String key = normalizedName + ":" + bucket;

        if (normalizedName.isEmpty()) {
            return new ProductLookup(null, "MOCK");
        }

        CachedProduct cached = productCache.get(key);
        if (cached != null && !cached.isExpired()) {
            // ✅ 캐시된 상품도 재검증 (Banned Keyword 체크)
            if (cached.product == null || categoryService.isValidProductTitle(cached.product.getProductName())) {
                log.info("[SHOPPING_LIST][{}] CACHE_HIT ingredient={} key={}", traceId, ingredientName, key);
                return new ProductLookup(cached.product, normalizeSource(cached.source));
            } else {
                 log.info("[SHOPPING_LIST][{}] CACHE_INVALIDATED ingredient={} reason=BANNED_KEYWORD", traceId, ingredientName);
            }
        }
        log.info("[SHOPPING_LIST][{}] CACHE_MISS ingredient={} key={} budget={}", traceId, ingredientName, key, allocatedBudget);

        ShoppingListResponse.ProductCard product = null;
        String source = "MOCK";

        try {
            // A2-R3 Call with allocatedBudget
            ShoppingService.SearchOneResult r = shoppingService.searchOne(ingredientName, allocatedBudget);
            if (r != null) {
                product = r.product();
                source = normalizeSource(r.source());
            }
        } catch (Exception e) {
            log.warn("[SHOPPING_LIST][{}] SEARCH_FAIL ingredient={} reason={}", traceId, ingredientName, e.getMessage());
        }

        long expireAt = System.currentTimeMillis() + PRODUCT_CACHE_TTL.toMillis();
        productCache.put(key, new CachedProduct(product, source, expireAt));

        return new ProductLookup(product, source);
    }

    private String normalizeCacheKey(String ingredientName) {
        if (ingredientName == null) return "";
        String key = ingredientName.trim().toLowerCase(Locale.ROOT);
        return key.replaceAll("\\s+", " ");
    }

    private String normalizeSource(String source) {
        return "REAL".equalsIgnoreCase(source) ? "REAL" : "MOCK";
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId == null || traceId.isBlank()) ? "no-trace" : traceId;
    }

    private String buildIngredientSummary(List<MealPlanIngredientResponse> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        int limit = Math.min(10, ingredients.size());
        for (int i = 0; i < limit; i++) {
            MealPlanIngredientResponse ing = ingredients.get(i);
            if (ing == null) continue;
            if (sb.length() > 1) sb.append(", ");
            String name = safeTrim(ing.getIngredientName());
            Long grams = toLongSafe(ing.getTotalGram());
            Integer count = ing.getDaysCount();
            sb.append("{name=").append(name)
                    .append(", gram=").append(grams == null ? 0 : grams)
                    .append(", count=").append(count == null ? 0 : count)
                    .append("}");
        }

        sb.append("]");
        return sb.toString();
    }

    private Integer toIntSafe(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLongSafe(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static class RangeWindow {
        LocalDate from;
        LocalDate to;
        RangeWindow(LocalDate f, LocalDate t) { from = f; to = t; }
    }

    private RangeWindow resolveRange(MealPlan plan, String range) {
        LocalDate today = LocalDate.now();
        String r = (range == null ? "MONTH" : range.toUpperCase());

        return switch (r) {
            case "TODAY" -> new RangeWindow(today, today);
            case "WEEK" -> new RangeWindow(today, today.plusDays(6));
            default -> new RangeWindow(plan.getStartDate(), plan.getEndDate());
        };
    }

    // =========================
    // 이하 기존 조회 메서드들
    // =========================

    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getMealPlan(Long planId) {
        MealPlan mealPlan = mealPlanMapper.findMealPlanById(planId);
        if (mealPlan == null) throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(planId);

        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();
        for (MealPlanDay day : days) {
            itemsByDayId.put(day.getId(), mealPlanMapper.findMealItemsByDayId(day.getId()));
        }

        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(day -> MealPlanDaySummaryResponse.from(day, itemsByDayId.getOrDefault(day.getId(), List.of())))
                .toList();

        return MealPlanOverviewResponse.of(mealPlan, daySummaries);
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanOverviewResponse getLatestMealPlanForUser(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) throw new BusinessException("해당 사용자의 최근 식단 플랜이 없습니다.");
        return getMealPlan(latestPlan.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId) {
        MealPlan plan = mealPlanMapper.findMealPlanById(planId);
        if (plan == null) throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);

        List<MealPlanIngredientResponse> list = mealPlanMapper.findIngredientsForPlan(planId);
        if (list == null || list.isEmpty()) throw new BusinessException("해당 플랜의 재료 정보가 없습니다. id=" + planId);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) throw new BusinessException("해당 사용자의 최근 식단 플랜이 없습니다.");

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(latestPlan.getId());
        if (days == null || days.isEmpty()) throw new BusinessException("해당 플랜의 날짜가 없습니다. planId=" + latestPlan.getId());

        int totalCalories = days.stream().mapToInt(MealPlanDay::getTotalCalories).sum();
        int averageCalories = (int) Math.round(totalCalories / (double) days.size());

        int targetPerDay = latestPlan.getTargetCaloriesPerDay();
        int achievementRate = (int) Math.round(totalCalories / (double) (targetPerDay * days.size()) * 100.0);

        // Calculate consumed calories for TODAY
        LocalDate today = LocalDate.now();
        MealPlanDay todayDay = days.stream()
                .filter(d -> d.getPlanDate().equals(today))
                .findFirst()
                .orElse(null);

        Integer consumedCalories = 0;
        Integer todayTargetCalories = targetPerDay;
        Integer todayAchievementRate = 0;

        if (todayDay != null) {
            // Check intakes
            List<MealIntake> intakes = mealIntakeMapper.findConsumedByDayId(userId, todayDay.getId());
            log.info("[Dashboard] todayDayId={} intakes.size={}", todayDay.getId(), intakes.size());
            
            Set<String> consumedMealTimes = intakes.stream()
                    .map(MealIntake::getMealTime)
                    .collect(Collectors.toSet());
            
            // Get items for today to sum consumed calories
            List<MealItem> todayItems = mealPlanMapper.findMealItemsByDayId(todayDay.getId());
            
            consumedCalories = todayItems.stream()
                    .filter(item -> consumedMealTimes.contains(item.getMealTime()))
                    .mapToInt(MealItem::getCalories)
                    .sum();
                    
            todayTargetCalories = todayDay.getTotalCalories() > 0 ? todayDay.getTotalCalories() : targetPerDay;
            
            if (todayTargetCalories > 0) {
                todayAchievementRate = (int) Math.round((double) consumedCalories / todayTargetCalories * 100);
            }
        }

        WeightRecord latest = weightRecordMapper.findLatestByUserId(userId);

        boolean hasWeightRecords = latest != null;
        Double latestWeight = (latest != null) ? latest.getWeight() : null;

        Double weightChange7Days = null;
        if (latest != null) {
            LocalDate d7 = latest.getRecordDate().minusDays(7);
            WeightRecord weight7 = weightRecordMapper.findByUserIdAndDate(userId, d7);
            if (weight7 != null) weightChange7Days = latest.getWeight() - weight7.getWeight();
        }

        return DashboardSummaryResponse.builder()
                .userId(userId)
                .recentMealPlanId(latestPlan.getId())
                .todayDayId(todayDay != null ? todayDay.getId() : null)
                .startDate(latestPlan.getStartDate())
                .endDate(latestPlan.getEndDate())
                .totalDays(latestPlan.getTotalDays())
                .targetCaloriesPerDay(targetPerDay)
                .averageCalories(averageCalories)
                .achievementRate(achievementRate)
                .consumedCalories(consumedCalories)
                .todayTargetCalories(todayTargetCalories)
                .todayAchievementRate(todayAchievementRate)
                .hasWeightRecords(hasWeightRecords)
                .latestWeight(latestWeight)
                .weightChange7Days(weightChange7Days)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanDayDetailResponse getDayDetail(Long dayId) {
        MealPlanDay day = mealPlanMapper.findMealPlanDayById(dayId);
        if (day == null) throw new BusinessException("존재하지 않는 Day 입니다. id=" + dayId);

        List<MealItem> items = mealPlanMapper.findMealItemsByDayId(dayId);
        
        // Find intakes
        MealPlan plan = mealPlanMapper.findMealPlanById(day.getMealPlanId());
        List<MealIntake> intakes = mealIntakeMapper.findByDayId(plan.getUserId(), dayId);
        log.info("[DayDetail] dayId={} intakes={}", dayId, intakes.stream().map(i -> i.getMealTime() + "=" + i.isConsumed()).collect(Collectors.joining(",")));

        Set<String> consumedMealTimes = intakes.stream()
                .filter(MealIntake::isConsumed)
                .map(MealIntake::getMealTime)
                .collect(Collectors.toSet());

        // Group by mealTime
        Map<String, List<MealItem>> itemsByMealTime = items.stream()
                .collect(Collectors.groupingBy(MealItem::getMealTime));
        
        // Sort meal times (Breakfast -> Lunch -> Dinner -> Snack)
        List<String> sortedMealTimes = itemsByMealTime.keySet().stream()
                .sorted(this::compareMealTimes)
                .toList();

        List<MealDetailResponse> meals = new ArrayList<>();
        for (String mt : sortedMealTimes) {
            List<MealItem> mealItems = itemsByMealTime.get(mt);
            int mealTotalCalories = mealItems.stream().mapToInt(MealItem::getCalories).sum();
            
            List<MealItemResponse> itemResponses = mealItems.stream()
                    .map(it -> MealItemResponse.builder()
                            .id(it.getId())
                            .mealTime(it.getMealTime())
                            .foodName(it.getFoodName())
                            .calories(it.getCalories())
                            .grams(it.getGrams())
                            .carbs(it.getCarbs())
                            .protein(it.getProtein())
                            .fat(it.getFat())
                            .isHighProtein(it.getIsHighProtein())
                            .memo(it.getMemo())
                            .build())
                    .toList();
            
            meals.add(MealDetailResponse.builder()
                    .mealTime(mt)
                    .totalCalories(mealTotalCalories)
                    .isConsumed(consumedMealTimes.contains(mt))
                    .items(itemResponses)
                    .build());
        }

        // Backward compatibility for flat list
        List<MealItemResponse> flatItems = items.stream()
                .map(it -> MealItemResponse.builder()
                        .id(it.getId())
                        .mealTime(it.getMealTime())
                        .foodName(it.getFoodName())
                        .calories(it.getCalories())
                        .grams(it.getGrams())
                        .carbs(it.getCarbs())
                        .protein(it.getProtein())
                        .fat(it.getFat())
                        .isHighProtein(it.getIsHighProtein())
                        .memo(it.getMemo())
                        .build())
                .toList();

        return MealPlanDayDetailResponse.builder()
                .dayId(day.getId())
                .date(day.getPlanDate().toString())
                .totalCalories(day.getTotalCalories())
                .items(flatItems)
                .meals(meals)
                .build();
    }
    
    private int compareMealTimes(String t1, String t2) {
        return getMealTimeOrder(t1) - getMealTimeOrder(t2);
    }
    
    private int getMealTimeOrder(String t) {
        if (t == null) return 99;
        switch (t.toUpperCase()) {
            case "BREAKFAST": return 1;
            case "LUNCH": return 2;
            case "DINNER": return 3;
            case "SNACK": return 4;
            default: return 99;
        }
    }
    @Override
    @Transactional
    public MealPlanDayDetailResponse regenerateDay(Long dayId) {
        MealPlanDay day = mealPlanMapper.findMealPlanDayById(dayId);
        if (day == null) throw new BusinessException("존재하지 않는 Day 입니다. id=" + dayId);

        MealPlan plan = mealPlanMapper.findMealPlanById(day.getMealPlanId());
        if (plan == null) throw new BusinessException("식단 플랜 정보를 찾을 수 없습니다.");

        // Clear intakes
        mealIntakeMapper.deleteByDayId(plan.getUserId(), dayId);

        // 1. 기존 데이터 삭제
        int deleted = mealPlanMapper.deleteMealItemsByDayId(dayId);
        log.info("[MealPlan] regenerateDay dayId={} deletedItems={}", dayId, deleted);

        // 2. AI 생성 (1일치)
        int targetKcal = plan.getTargetCaloriesPerDay();
        int mealsPerDay = plan.getMealsPerDay();
        
        List<MealItem> newItems = generateNewItemsForDay(day, plan, targetKcal, mealsPerDay, null);

        // 3. 저장
        for (MealItem item : newItems) {
            mealPlanMapper.insertMealItem(item);
        }

        // 4. 칼로리 업데이트
        updateDayTotalCalories(day);

        return getDayDetail(dayId);
    }

    @Override
    @Transactional
    public MealPlanDayDetailResponse replaceMeal(Long dayId, String mealTime) {
        MealPlanDay day = mealPlanMapper.findMealPlanDayById(dayId);
        if (day == null) throw new BusinessException("존재하지 않는 Day 입니다. id=" + dayId);

        String normalizedMealTime = normalizeMealTime(mealTime);
        if (normalizedMealTime == null) throw new BusinessException("유효하지 않은 끼니입니다: " + mealTime);

        MealPlan plan = mealPlanMapper.findMealPlanById(day.getMealPlanId());
        if (plan == null) throw new BusinessException("식단 플랜 정보를 찾을 수 없습니다.");

        // Clear intakes
        mealIntakeMapper.deleteByDayIdAndMealTime(plan.getUserId(), dayId, normalizedMealTime);

        // 1. 해당 끼니 삭제
        int deleted = mealPlanMapper.deleteMealItemsByDayIdAndMealTime(dayId, normalizedMealTime);
        log.info("[MealPlan] replaceMeal dayId={} mealTime={} deletedItems={}", dayId, normalizedMealTime, deleted);

        // 2. AI 생성 (1일치 생성 후 해당 끼니만 추출)
        // 효율성을 위해 전체 하루를 생성하고 필요한 끼니만 골라냅니다.
        int targetKcal = plan.getTargetCaloriesPerDay();
        int mealsPerDay = plan.getMealsPerDay();

        // 타겟 끼니만 교체하기 위해 generateNewItemsForDay 내부 로직을 활용하되,
        // 결과에서 해당 mealTime만 필터링
        List<MealItem> allNewItems = generateNewItemsForDay(day, plan, targetKcal, mealsPerDay, normalizedMealTime);
        
        // 생성된 항목 중 타겟 mealTime만 선택
        List<MealItem> targetItems = allNewItems.stream()
                .filter(item -> item.getMealTime().equals(normalizedMealTime))
                .toList();
        
        log.info("[MealPlan] replaceMeal generated items for insert={}", targetItems.size());

        // 3. 저장
        for (MealItem item : targetItems) {
            mealPlanMapper.insertMealItem(item);
        }

        // 4. 칼로리 업데이트
        updateDayTotalCalories(day);

        return getDayDetail(dayId);
    }

    private List<MealItem> generateNewItemsForDay(MealPlanDay day, MealPlan plan, int targetKcal, int mealsPerDay, String specificMealTime) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("startDate", day.getPlanDate().format(DF));
        payload.put("totalDays", 1);
        payload.put("targetCaloriesPerDay", targetKcal);
        payload.put("mealsPerDay", mealsPerDay);
        payload.put("monthlyBudget", plan.getMonthlyBudget());
        // String -> List 변환 필요 (toCsvSafe 역변환)
        // 여기서는 간단히 빈 리스트 또는 plan에 저장된 값 파싱 (현재 DB엔 CSV로 저장됨)
        // 파싱 로직이 복잡하니 일단 빈 리스트로 보냄 (재생성 시 선호도는 생략 or 구현 필요)
        // 실제로는 plan.getPreferences() 등을 파싱해서 넣어야 함. 
        // 편의상 빈 리스트로 진행 (AI가 알아서 추천)
        payload.put("preferences", List.of()); 
        payload.put("allergies", List.of());

        AiMonthlySkeletonResponse skeleton = null;
        try {
            skeleton = dietAiClient.generateMonthlySkeleton(payload);
        } catch (Exception e) {
            log.warn("[Regenerate] AI failed, fallback to template. dayId={}, reason={}", day.getId(), e.getMessage());
        }

        List<String> mealTimesForDay = switch (mealsPerDay) {
            case 1 -> List.of("LUNCH");
            case 2 -> List.of("BREAKFAST", "DINNER");
            default -> List.of("BREAKFAST", "LUNCH", "DINNER");
        };
        Map<String, Integer> mealTargets = distributeCalories(targetKcal, mealTimesForDay);

        List<MealItem> itemsForDay;
        if (skeleton != null && skeleton.getDays() != null && !skeleton.getDays().isEmpty()) {
            itemsForDay = buildItemsFromAiWithFixedRules(
                    skeleton,
                    0, // 1일치 생성했으므로 index 0
                    day.getId(),
                    mealsPerDay,
                    mealTargets
            );
        } else {
            itemsForDay = buildItemsFromTemplate(day.getId(), targetKcal, mealsPerDay);
        }
        
        return itemsForDay;
    }

    private void updateDayTotalCalories(MealPlanDay day) {
        int totalCaloriesForDay = 0;
        Integer sumFromDb = mealPlanMapper.sumMealItemCaloriesByDayId(day.getId());
        if (sumFromDb != null) {
            totalCaloriesForDay = sumFromDb;
        }
        day.setTotalCalories(totalCaloriesForDay);
        mealPlanMapper.updateMealPlanDayTotalCalories(day.getId(), totalCaloriesForDay);
    }
}

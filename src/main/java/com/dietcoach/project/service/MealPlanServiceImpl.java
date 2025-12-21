package com.dietcoach.project.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.client.ai.DietAiClient;
import com.dietcoach.project.client.ai.dto.AiMonthlySkeletonResponse;
import com.dietcoach.project.common.TdeeCalculator;
import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.WeightRecord;
import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlan;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealItemResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.dto.meal.ShoppingListResponse;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.WeightRecordMapper;
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

    // ✅ A2 캐시 TTL 1시간
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(1);

    private final UserMapper userMapper;
    private final MealPlanMapper mealPlanMapper;
    private final WeightRecordMapper weightRecordMapper;

    // ✅ AI Client
    private final DietAiClient dietAiClient;

    // ✅ 11번가 대표상품 1개 조회 서비스 (아래 ShoppingService.java 계약 참고)
    private final ShoppingService shoppingService;

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
            new FoodPortion(new FoodItem("그릭 요거트", 60), 150)
    );

    private static final List<FoodPortion> LUNCH_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("현미밥", 150), 200),
            new FoodPortion(new FoodItem("닭가슴살", 165), 150),
            new FoodPortion(new FoodItem("샐러드", 40), 80)
    );

    private static final List<FoodPortion> DINNER_TEMPLATE = List.of(
            new FoodPortion(new FoodItem("현미밥", 150), 150),
            new FoodPortion(new FoodItem("연어", 200), 120),
            new FoodPortion(new FoodItem("샐러드", 40), 80)
    );

    // =========================
    // 1) 한 달 식단 생성 (단일 진입점)
    // =========================
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

        AiMonthlySkeletonResponse skeleton = null;
        String generationSource = "TEMPLATE";

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("startDate", startDate.format(DF));
            payload.put("totalDays", DEFAULT_PLAN_DAYS);
            payload.put("targetCaloriesPerDay", targetKcalPerDay);
            payload.put("mealsPerDay", mealsPerDay);
            payload.put("monthlyBudget", request != null ? request.getMonthlyBudget() : null);
            payload.put("preferences", request != null ? request.getPreferences() : List.of());
            payload.put("allergies", request != null ? request.getAllergies() : List.of());

            log.info("[MealPlan] AI call start planId={}, userId={}", mealPlan.getId(), userId);
            skeleton = generateAiSkeletonInChunks(mealPlan.getId(), startDate, DEFAULT_PLAN_DAYS, payload);
            if (skeleton == null) {
                log.warn("[MealPlan] AI returned null, fallback to template. planId={}", mealPlan.getId());
            } else {
                validateAiSkeleton(skeleton);
                logAiResponseSummary(skeleton);
                int dayCount = (skeleton.getDays() == null ? 0 : skeleton.getDays().size());
                log.info("[MealPlan] AI call success planId={}, days={}", mealPlan.getId(), dayCount);
                generationSource = "AI";
            }
        } catch (BusinessException e) {
            log.warn("[MealPlan] AI invalid, fallback to template. planId={}, reason={}",
                    mealPlan.getId(), e.getMessage());
            skeleton = null;
        } catch (Exception e) {
            log.warn("[MealPlan] AI failed, fallback to template. planId={}, reason={}",
                    mealPlan.getId(), e.getMessage());
            skeleton = null;
        }

        List<MealPlanDay> days = new ArrayList<>();
        Map<Long, List<MealItem>> itemsByDayId = new HashMap<>();

        for (int i = 0; i < DEFAULT_PLAN_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);

            MealPlanDay day = MealPlanDay.builder()
                    .mealPlanId(mealPlan.getId())
                    .planDate(date)
                    .dayIndex(i + 1)
                    .totalCalories(0)
                    .build();

            mealPlanMapper.insertMealPlanDay(day);
            days.add(day);

            List<MealItem> itemsForDay;
            List<String> mealTimesForDay = switch (mealsPerDay) {
                case 1 -> List.of("LUNCH");
                case 2 -> List.of("BREAKFAST", "DINNER");
                default -> List.of("BREAKFAST", "LUNCH", "DINNER");
            };
            Map<String, Integer> mealTargets = distributeCalories(targetKcalPerDay, mealTimesForDay);

            if (skeleton != null) {
                itemsForDay = buildItemsFromAiWithFixedRules(
                        skeleton,
                        i,
                        day.getId(),
                        mealsPerDay,
                        mealTargets
                );
                if (itemsForDay.isEmpty()) {
                    log.warn("[MealPlan] AI items empty planId={}, dayIndex={}", mealPlan.getId(), i + 1);
                }
            } else {
                itemsForDay = buildItemsFromTemplate(day.getId(), targetKcalPerDay, mealsPerDay);
            }

            for (MealItem item : itemsForDay) {
                mealPlanMapper.insertMealItem(item);
            }

            int totalCaloriesForDay = 0;
            Integer sumFromDb = mealPlanMapper.sumMealItemCaloriesByDayId(day.getId());
            if (sumFromDb != null) {
                totalCaloriesForDay = sumFromDb;
            }

            day.setTotalCalories(totalCaloriesForDay);
            mealPlanMapper.updateMealPlanDayTotalCalories(day.getId(), totalCaloriesForDay);

            if (skeleton != null && skeleton.getDays() != null && skeleton.getDays().size() > i) {
                AiMonthlySkeletonResponse.AiDaySkeleton aiDay = skeleton.getDays().get(i);
                if (aiDay != null && aiDay.getTotalCalories() != null) {
                    int diff = totalCaloriesForDay - aiDay.getTotalCalories();
                    if (diff != 0) {
                        log.info("[DAY_CAL_DIFF] dayIndex={}, ai={}, server={}, diff={}",
                                i + 1, aiDay.getTotalCalories(), totalCaloriesForDay, diff);
                    }
                }
            }

            itemsByDayId.put(day.getId(), itemsForDay);
        }

        List<MealPlanDaySummaryResponse> daySummaries = days.stream()
                .map(d -> MealPlanDaySummaryResponse.from(d, itemsByDayId.getOrDefault(d.getId(), List.of())))
                .toList();

        MealPlanOverviewResponse resp = MealPlanOverviewResponse.of(mealPlan, daySummaries);
        log.info("[MealPlan] created planId={}, source={}", mealPlan.getId(), generationSource);
        return resp;
    }

    private AiMonthlySkeletonResponse generateAiSkeletonInChunks(
            Long planId,
            LocalDate startDate,
            int totalDays,
            Map<String, Object> basePayload
    ) {
        List<AiMonthlySkeletonResponse.AiDaySkeleton> merged = new ArrayList<>();

        for (int offset = 0; offset < totalDays; offset += AI_CHUNK_DAYS) {
            int chunkDays = Math.min(AI_CHUNK_DAYS, totalDays - offset);
            LocalDate chunkStart = startDate.plusDays(offset);

            Map<String, Object> payload = new HashMap<>(basePayload);
            payload.put("startDate", chunkStart.format(DF));
            payload.put("totalDays", chunkDays);

            log.info("[MealPlan] AI chunk start planId={}, chunkStart={}, chunkDays={}",
                    planId, chunkStart, chunkDays);

            AiMonthlySkeletonResponse chunk = dietAiClient.generateMonthlySkeleton(payload);
            if (chunk == null || chunk.getDays() == null || chunk.getDays().isEmpty()) {
                log.warn("[MealPlan] AI chunk empty planId={}, chunkStart={}", planId, chunkStart);
                return null;
            }

            List<AiMonthlySkeletonResponse.AiDaySkeleton> days = chunk.getDays();
            for (int i = 0; i < chunkDays; i++) {
                AiMonthlySkeletonResponse.AiDaySkeleton day = (i < days.size()) ? days.get(i) : null;
                if (day == null) {
                    log.warn("[MealPlan] AI chunk missing day planId={}, chunkStart={}, dayOffset={}",
                            planId, chunkStart, i);
                    return null;
                }
                merged.add(AiMonthlySkeletonResponse.AiDaySkeleton.builder()
                        .dayIndex(offset + i + 1)
                        .planDate(chunkStart.plusDays(i).format(DF))
                        .meals(day.getMeals())
                        .totalCalories(day.getTotalCalories())
                        .build());
            }
        }

        return AiMonthlySkeletonResponse.builder()
                .days(merged)
                .build();
    }

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

            Integer mealTargetCalories = (mealTargets == null ? null : mealTargets.get(mt));
            String menuName = safeTrim(meal.getMenuName());
            List<AiMonthlySkeletonResponse.AiIngredient> ings =
                    (meal.getIngredients() == null) ? List.of() : meal.getIngredients();
            int ingredientCount = ings.size();

            for (AiMonthlySkeletonResponse.AiIngredient ing : ings) {
                if (ing == null) continue;
                String ingredient = safeTrim(ing.getIngredientName());
                if (ingredient.isEmpty()) continue;

                Integer calories = adjustCalories(ing.getCalories(), ingredient, mealTargetCalories, ingredientCount);
                Integer grams = adjustGrams(ing.getGrams(), ingredient, calories);
                String memo = menuName.isEmpty() ? "AI" : menuName;

                result.add(MealItem.builder()
                        .mealPlanDayId(mealPlanDayId)
                        .mealTime(mt)
                        .foodName(ingredient)
                        .grams(grams)
                        .calories(calories)
                        .memo(memo)
                        .build());
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

    private Integer adjustGrams(Integer grams, String ingredientName, Integer calories) {
        if (grams != null && grams > 0) {
            int adjusted = clamp(grams, 10, 800);
            if (adjusted != grams) {
                log.info("[GRAMS_CLAMP] ingredient={}, {} -> {}", ingredientName, grams, adjusted);
            }
            return adjusted;
        }

        if (calories != null && calories > 0) {
            double kcalPerGram = kcalPerGramFor(ingredientName);
            int derived = (int) Math.round(calories / kcalPerGram);
            int clamped = clamp(derived, 10, 800);
            log.info("[GRAMS_DERIVE] ingredient={}, cal={}, kpg={} -> {}",
                    ingredientName, calories, String.format(Locale.US, "%.2f", kcalPerGram), clamped);
            return clamped;
        }

        int fallback = 100;
        log.info("[GRAMS_DEFAULT] ingredient={}, null -> {}", ingredientName, fallback);
        return fallback;
    }
            return adjusted;
        }

        if (calories != null && calories > 0) {
            double kcalPerGram = kcalPerGramFor(ingredientName);
            int derived = (int) Math.round(calories / kcalPerGram);
            int clamped = clamp(derived, 10, maxGrams);
            log.info("[GRAMS_DERIVE] ingredient={}, cal={}, kpg={} -> {}",
                    ingredientName, calories, String.format(Locale.US, "%.2f", kcalPerGram), clamped);
            return clamped;
        }

        int fallback = 100;
        log.info("[GRAMS_DEFAULT] ingredient={}, null -> {}", ingredientName, fallback);
        return fallback;
    }
            return adjusted;
        }

        if (calories != null && calories > 0) {
            double kcalPerGram = kcalPerGramFor(ingredientName);
            int derived = (int) Math.round(calories / kcalPerGram);
            int clamped = clamp(derived, 10, 600);
            log.info("[GRAMS_DERIVE] ingredient={}, cal={}, kpg={} -> {}",
                    ingredientName, calories, String.format(Locale.US, "%.2f", kcalPerGram), clamped);
            return clamped;
        }

        int fallback = 100;
        log.info("[GRAMS_DEFAULT] ingredient={}, null -> {}", ingredientName, fallback);
        return fallback;
    }
            return adjusted;
        }

        if (calories != null && calories > 0) {
            double kcalPerGram = kcalPerGramFor(ingredientName);
            int derived = (int) Math.round(calories / kcalPerGram);
            int clamped = clamp(derived, 10, 800);
            log.info("[GRAMS_DERIVE] ingredient={}, cal={}, kpg={} -> {}",
                    ingredientName, calories, String.format(Locale.US, "%.2f", kcalPerGram), clamped);
            return clamped;
        }

        int fallback = 100;
        log.info("[GRAMS_DEFAULT] ingredient={}, null -> {}", ingredientName, fallback);
        return fallback;
    }

    private Integer adjustCalories(Integer calories, String ingredientName, Integer mealCalories, int ingredientCount) {
        if (calories == null) {
            if (mealCalories != null && ingredientCount > 0) {
                int derived = (int) Math.round(mealCalories / (double) ingredientCount);
                int clamped = clamp(derived, 0, 2000);
                log.info("[CAL_DERIVE] ingredient={}, mealCal={}, count={} -> {}",
                        ingredientName, mealCalories, ingredientCount, clamped);
                return clamped;
            }
            return 0;
        }
        int adjusted = clamp(calories, 0, 2000);
        if (adjusted != calories) {
            log.info("[CAL_CLAMP] ingredient={}, {} -> {}", ingredientName, calories, adjusted);
        }
        return adjusted;
    }

    private double kcalPerGramFor(String ingredientName) {
        String n = (ingredientName == null ? "" : ingredientName.trim().toLowerCase());

        if (containsAny(n,
                "oil", "olive", "olive oil", "butter", "mayo", "cream", "cheese", "nuts",
                "??", "??", "????", "???", "???", "???",
                "??", "??", "????", "??",
                "??", "??", "???", "??", "??")) {
            return 7.5;
        }
        if (containsAny(n,
                "rice", "noodle", "ramen", "pasta", "bread", "oat", "oatmeal", "potato", "sweet potato",
                "?", "?", "??", "??", "??", "?", "??", "???", "?", "???", "??", "???")) {
            return 2.5;
        }
        if (containsAny(n,
                "chicken", "beef", "pork", "fish", "salmon", "tuna", "shrimp", "egg", "tofu",
                "?", "????", "???", "???", "????", "??", "??", "??", "??", "??", "??", "??")) {
            return 2.0;
        }
        if (containsAny(n,
                "salad", "lettuce", "spinach", "broccoli", "cabbage", "tomato", "cucumber", "carrot",
                "apple", "banana", "grape", "orange", "berry",
                "???", "???", "??", "???", "????", "???", "???", "??", "??",
                "??", "???", "??", "???", "??")) {
            return 0.25;
        }
        if (containsAny(n,
                "sauce", "soy sauce", "gochujang", "doenjang", "ketchup",
                "??", "??", "???", "??", "??")) {
            return 1.0;
        }
        return 1.0;
    }

private boolean containsAny(String value, String... tokens) {
        if (value == null) return false;
        for (String token : tokens) {
            if (token == null) continue;
            if (value.contains(token.toLowerCase())) return true;
        }
        return false;
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
    // ✅ A2) 장보기 리스트 + 대표상품 매핑(완료)
    // - DTO: ShoppingListResponse(주인님 제공 버전) 그대로 사용
    // =========================
    @Override
    @Transactional(readOnly = true)
    public ShoppingListResponse getShoppingList(Long planId, String range) {
        MealPlan plan = mealPlanMapper.findMealPlanById(planId);
        if (plan == null) throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);

        RangeWindow w = resolveRange(plan, range);

        List<MealPlanIngredientResponse> ingredients =
                mealPlanMapper.findIngredientsForPlanInRange(planId, w.from, w.to);

        List<ShoppingListResponse.ShoppingItem> items = new ArrayList<>();

        for (MealPlanIngredientResponse ing : ingredients) {
            String ingredientName = safeTrim(ing.getIngredientName());
            if (ingredientName.isEmpty()) continue;

            ProductLookup lookup = getRepresentativeProductCached(ingredientName);

            items.add(ShoppingListResponse.ShoppingItem.builder()
                    .ingredientName(ingredientName)
                    .totalGram(toLongSafe(ing.getTotalGram()))
                    .daysCount(ing.getDaysCount())
                    .product(lookup.product)
                    .source(lookup.source)
                    .build());
        }

        return ShoppingListResponse.builder()
                .planId(planId)
                .range((range == null ? "MONTH" : range.toUpperCase()))
                .fromDate(w.from)
                .toDate(w.to)
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

    private ProductLookup getRepresentativeProductCached(String ingredientName) {
        String key = ingredientName.toLowerCase();

        CachedProduct cached = productCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return new ProductLookup(cached.product, cached.source);
        }

        ShoppingListResponse.ProductCard product = null;
        String source = "NONE";

        try {
            ShoppingService.SearchOneResult r = shoppingService.searchOne(ingredientName);
            if (r != null && r.product() != null) {
                product = r.product();
                source = (r.source() == null ? "REAL" : r.source());
            }
        } catch (Exception e) {
            log.warn("[Shopping] 대표상품 매핑 실패 ingredient={}, reason={}", ingredientName, e.getMessage());
        }

        long expireAt = System.currentTimeMillis() + PRODUCT_CACHE_TTL.toMillis();
        productCache.put(key, new CachedProduct(product, source, expireAt));

        return new ProductLookup(product, source);
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
            case "WEEK" -> new RangeWindow(today, today.plusDays(6)); // 고정 룰
            default -> new RangeWindow(plan.getStartDate(), plan.getEndDate());
        };
    }

    // =========================
    // 이하 기존 메서드들 (주인님 기존 구현 유지)
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
        if (latestPlan == null) throw new BusinessException("해당 유저의 최근 식단 플랜이 없습니다. userId=" + userId);
        return getMealPlan(latestPlan.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId) {
        MealPlan plan = mealPlanMapper.findMealPlanById(planId);
        if (plan == null) throw new BusinessException("존재하지 않는 식단 플랜입니다. id=" + planId);

        List<MealPlanIngredientResponse> list = mealPlanMapper.findIngredientsForPlan(planId);
        if (list == null || list.isEmpty()) throw new BusinessException("해당 플랜에 재료 정보가 없습니다. id=" + planId);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        MealPlan latestPlan = mealPlanMapper.findLatestMealPlanByUserId(userId);
        if (latestPlan == null) throw new BusinessException("해당 유저의 최근 식단 플랜이 없습니다. userId=" + userId);

        List<MealPlanDay> days = mealPlanMapper.findMealPlanDaysByPlanId(latestPlan.getId());
        if (days == null || days.isEmpty()) throw new BusinessException("해당 플랜에 포함된 날짜가 없습니다. planId=" + latestPlan.getId());

        int totalCalories = days.stream().mapToInt(MealPlanDay::getTotalCalories).sum();
        int averageCalories = (int) Math.round(totalCalories / (double) days.size());

        int targetPerDay = latestPlan.getTargetCaloriesPerDay();
        int achievementRate = (int) Math.round(totalCalories / (double) (targetPerDay * days.size()) * 100.0);

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
                .startDate(latestPlan.getStartDate())
                .endDate(latestPlan.getEndDate())
                .totalDays(latestPlan.getTotalDays())
                .targetCaloriesPerDay(targetPerDay)
                .averageCalories(averageCalories)
                .achievementRate(achievementRate)
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

        List<MealItemResponse> itemResponses = items.stream()
                .map(it -> MealItemResponse.builder()
                        .id(it.getId())
                        .mealTime(it.getMealTime())
                        .foodName(it.getFoodName())
                        .calories(it.getCalories())
                        .grams(it.getGrams())
                        .memo(it.getMemo())
                        .build())
                .toList();

        return MealPlanDayDetailResponse.builder()
                .dayId(day.getId())
                .date(day.getPlanDate().toString())
                .totalCalories(day.getTotalCalories())
                .items(itemResponses)
                .build();
    }
}

# Meal Plan AI - Change Log (2025-12-21)

## Summary
- Added startup diagnostics to confirm active profile, build info, and bean classes.
- AI monthly plan generation now runs in 7-day chunks to reduce timeouts.
- AI response parsing accepts both ingredient strings and objects (ingredientName/grams/calories).
- Ingredient-level persistence with server-side normalization for grams/calories.
- Daily total calories are now computed by DB SUM and stored as the source of truth.
- System prompt updated to enforce Korean menu and ingredient names.
- API timeouts increased for AI calls and frontend requests.

## Backend changes

### Service layer
- File: src/main/java/com/dietcoach/project/service/MealPlanServiceImpl.java
  - Chunked AI calls (7 days) for monthly plan generation.
  - Saved items per ingredient with server-side grams/calories normalization.
  - total_calories updated via DB SUM after insert.
  - Added logs:
    - AI response summary (days/ingredients/gramsMissing/caloriesMissing)
    - [GRAMS_CLAMP] / [CAL_CLAMP] for corrections
    - [CAL_DERIVE] / [GRAMS_DERIVE] for derived values
    - [DAY_CAL_DIFF] AI vs server total comparison

### AI client
- File: src/main/java/com/dietcoach/project/client/ai/DietAiClient.java
  - System prompt forces Korean output for menuName and ingredientName.
  - Added request/response diagnostics (model, body size, key presence).

### AI DTO parsing
- File: src/main/java/com/dietcoach/project/client/ai/dto/AiMonthlySkeletonResponse.java
  - Accepts ingredients as either:
    - list of strings, or
    - list of objects with ingredientName/grams/calories.
  - Supports day.totalCalories field in response.

### Mapper changes
- File: src/main/java/com/dietcoach/project/mapper/meal/MealPlanMapper.java
- File: src/main/resources/mapper/meal/MealPlanMapper.xml
  - Added sumMealItemCaloriesByDayId query for daily totals.

### DTO response
- File: src/main/java/com/dietcoach/project/dto/meal/MealPlanDaySummaryResponse.java
  - Added memo field handling and daily total fallback.

### Config
- File: src/main/resources/application-dev.yml
  - gms.openai.timeout-ms set to 120000 (120s).

## Frontend changes
- File: frontend/src/api/mealPlanApi.js
  - Increased createMealPlan request timeout to 120s.

## Runtime behavior
- AI values are treated as hints only.
- grams/calories are normalized on server and stored as final values.
- meal_plan_days.total_calories is always DB SUM.
- TEMPLATE fallback is only used when AI parsing fails or required fields are missing.

## Verification signals
- Startup logs show active profiles, build info, MealPlanServiceImpl and DietAiClient classes.
- Meal plan creation logs include:
  - AI call start
  - AI response summary
  - correction/derive logs
  - daily total updates
  - fallback reason if any

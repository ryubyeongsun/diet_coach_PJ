# dietCoach Project - System Documentation

## 1) Overview
- Domain: diet coaching (meal plans, weight tracking, dashboard, shopping).
- Tech: Spring Boot 3.x, MyBatis, MySQL, JWT auth, Vue frontend (frontend folder).
- Backend base package: com.dietcoach.project

## 2) Package Structure (Backend)
- com.dietcoach.project
  - client
    - ai: DietAiClient (AI meal plan generation)
    - shopping: shopping API client (mock products included)
  - config: security, startup diagnostics, web config
  - controller: REST endpoints (auth, user, meal plan, dashboard, weight, shopping, health)
  - domain: JPA-style domain objects and enums
    - meal: MealPlan, MealPlanDay, MealItem
    - auth: RefreshToken
    - enums: Gender, ActivityLevel, GoalType
  - dto
    - auth: request/response DTOs for login/signup/refresh
    - user: profile and TDEE responses
    - meal: meal plan requests/responses, shopping list
    - dashboard: trend responses
    - weight: weight record requests/responses
    - shopping: product responses
  - mapper: MyBatis mappers and XML
  - security: JWT filters, user details
  - service: business logic (MealPlanServiceImpl, UserService, AuthService, etc.)

## 3) Data Model (MySQL)
- users: profile, activity, goal, target_calories
- weight_records: per-day weights
- meal_plans: monthly plan meta (start/end, totals, preferences/allergies)
- meal_plan_days: per-day totals
- meal_items: per-ingredient rows (food_name, grams, calories, memo)
- shopping_cart_items: shopping items tied to plan/day
- refresh_tokens: JWT refresh tokens

## 4) Implemented Features
- Auth: signup/login, refresh token, me endpoint
- User: profile update, TDEE calculation
- Meal Plan: create monthly plan (AI + fallback), list latest plan, plan detail, day detail
- Dashboard: summary and trend (calories + weight)
- Weight: upsert, list, delete
- Shopping: ingredient aggregation + product lookup (real or mock)
- Health: liveness endpoint

## 5) API Specification (Backend)

### Auth
- POST /api/auth/signup
  - Req: SignupRequest { email, name, password }
  - Res: ApiResponse<AuthResponse>
- POST /api/auth/login
  - Req: LoginRequest { email, password }
  - Res: ApiResponse<AuthResponse>
- POST /api/auth/refresh
  - Req: RefreshRequest { refreshToken }
  - Res: ApiResponse<AuthResponse>
- GET /api/auth/me
  - Auth: Bearer token
  - Res: ApiResponse<UserProfileResponse>

### User
- POST /api/users
  - Req: UserCreateRequest { email, password, name, gender, birthDate, height, weight, activityLevel, goalType }
  - Res: ApiResponse<UserProfileResponse>
- GET /api/users/{id}
  - Res: ApiResponse<UserProfileResponse>
- PUT /api/users/{id}/profile
  - Req: UserProfileUpdateRequest { gender, birthDate, height, weight, activityLevel, goalType }
  - Res: ApiResponse<UserProfileResponse>
- GET /api/users/{id}/tdee
  - Res: ApiResponse<TdeeResponse>

### Meal Plan
- POST /api/meal-plans
  - Req: MealPlanCreateRequest { userId, startDate, monthlyBudget, mealsPerDay, preferences[], allergies[] }
  - Res: ApiResponse<MealPlanOverviewResponse>
- GET /api/meal-plans/{planId}
  - Res: ApiResponse<MealPlanOverviewResponse>
- GET /api/meal-plans/latest
  - Res: ApiResponse<MealPlanOverviewResponse>
- GET /api/users/{userId}/meal-plans/latest
  - Res: ApiResponse<MealPlanOverviewResponse>
- GET /api/meal-plans/days/{dayId}
  - Res: ApiResponse<MealPlanDayDetailResponse>
- GET /api/meal-plans/{planId}/ingredients
  - Res: ApiResponse<List<MealPlanIngredientResponse>>
- GET /api/meal-plans/{planId}/shopping?range=MONTH
  - Res: ApiResponse<ShoppingListResponse>
- GET /api/dashboard/summary
  - Res: ApiResponse<DashboardSummaryResponse>

### Dashboard
- GET /api/dashboard/trend?userId=&from=&to=
  - Res: ApiResponse<DashboardTrendResponse>

### Weight
- POST /api/users/{userId}/weights
  - Req: WeightRecordUpsertRequest { recordDate, weight, memo }
  - Res: ApiResponse<WeightRecordResponse>
- GET /api/users/{userId}/weights?from=&to=
  - Res: ApiResponse<WeightRecordListResponse>
- DELETE /api/users/{userId}/weights/{recordId}
  - Res: ApiResponse<Void>

### Shopping
- GET /api/shopping/search?keyword=&page=1&size=10
  - Res: ApiResponse<List<ShoppingProductResponse>>

### Health
- GET /api/health
  - Res: ApiResponse<String>

## 6) DTO Field Notes (Key Responses)
- MealPlanOverviewResponse: mealPlanId, userId, startDate, endDate, targetCaloriesPerDay, days[], monthlyBudget, mealsPerDay, preferences, allergies, dailyBudgetGuide
- MealPlanDaySummaryResponse: dayId, date, totalCalories, label, memo (AI or TEMPLATE)
- MealPlanDayDetailResponse: dayId, date, totalCalories, items[]
- MealItemResponse: id, mealTime, foodName, calories, grams, memo
- MealPlanIngredientResponse: ingredientName, totalGram, totalCalories, daysCount
- ShoppingListResponse: planId, range, fromDate, toDate, items[] (ingredientName, totalGram, daysCount, product, source)
- DashboardTrendResponse: hasData, fromDate, toDate, dayTrends[]
- DayTrend: date, totalCalories, targetCalories, weight, achievementRate
- DashboardSummaryResponse: userId, recentMealPlanId, startDate, endDate, totalDays, latestWeight, weightChange7Days, hasWeightRecords, targetCaloriesPerDay, averageCalories, achievementRate

## 7) Config and Profiles
- application.yml sets profile=dev by default
- application-dev.yml uses local MySQL and includes JWT secret and gms.openai base-url/api-key/timeout
- application-prod.yml expects env vars

## 8) Meal Plan AI Storage Rules (Implemented)
- AI response parsing is active; AI data is treated as hints.
- Storage uses ingredient-level rows in meal_items.
- grams clamp: null allowed, <10 -> 10, >800 -> 800, negative -> null
- calories clamp: null -> 0, <0 -> 0, >2000 -> 2000
- day total calories are always server SUM(meal_items.calories)
- Logs include: AI response summary, clamp logs, day calories diff logs
- Fallback to TEMPLATE only when parsing/validation fails (not on partial save errors)

## 9) Build and Run (Backend)
- Build: .\mvnw clean package -DskipTests
- Run: java -jar target\dietCoach-0.0.1-SNAPSHOT.jar

## 10) Frontend
- Vue app lives in frontend
- Typical dev run: npm run dev (from frontend)

## 11) Known Behavior Notes
- Meal plan creation triggers AI call then DB write; long AI calls can cause client timeouts if output is large.
- DB integrity: meal_items.grams is NOT NULL; ensure server sets grams when AI omits it.

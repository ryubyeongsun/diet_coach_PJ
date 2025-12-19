# API 명세서

이 문서는 'Diet Coach' 백엔드 애플리케이션의 RESTful API 명세를 상세히 정의합니다. 모든 요청과 응답은 JSON 형식을 따르며, 모든 API는 `/api` 경로 하위에 위치합니다.

-   **공통 응답 구조**: 모든 API 응답은 아래와 같이 `ApiResponse` 객체로 감싸여 반환됩니다.
    ```json
    {
        "message": "응답 메시지",
        "data": { ... } // 실제 데이터 객체 또는 null
    }
    ```
-   **인증**: `/api/auth` 경로를 제외한 모든 API는 HTTP 요청 헤더에 `Authorization: Bearer <Access_Token>` 형식의 JWT 토큰을 포함해야 합니다.

---

## 1. 인증 (Auth)

**Controller**: `AuthController`

### 1.1. 회원가입

-   **Endpoint**: `POST /api/auth/signup`
-   **설명**: 새로운 사용자를 시스템에 등록합니다.
-   **Request Body**:
    ```json
    {
        "email": "user@example.com",
        "password": "password123",
        "name": "홍길동"
    }
    ```
-   **Success Response** (201 Created):
    -   `message`: "회원가입이 완료되었습니다."
    -   `data`: (User-ID, Long)

### 1.2. 로그인

-   **Endpoint**: `POST /api/auth/login`
-   **설명**: 사용자 인증을 수행하고, 성공 시 JWT 토큰을 발급합니다.
-   **Request Body**:
    ```json
    {
        "email": "user@example.com",
        "password": "password123"
    }
    ```
-   **Success Response** (200 OK):
    -   `message`: "로그인 성공"
    -   `data`:
        ```json
        {
            "grantType": "Bearer",
            "accessToken": "ey...",
            "refreshToken": "ey..."
        }
        ```

---

## 2. 사용자 (User)

**Controller**: `UserController`

### 2.1. 사용자 정보 조회

-   **Endpoint**: `GET /api/users/{userId}`
-   **설명**: 특정 사용자의 프로필 정보를 조회합니다.
-   **Success Response** (200 OK):
    -   `data`: UserProfile DTO (id, email, name, gender, birthDate, height, weight 등)

### 2.2. TDEE 정보 조회

-   **Endpoint**: `GET /api/users/{userId}/tdee`
-   **설명**: 사용자의 신체 정보를 바탕으로 BMR, TDEE, 목표 칼로리 등을 계산하여 반환합니다.
-   **Success Response** (200 OK):
    -   `data`: TdeeResponse DTO (bmr, tdee, targetCalories 등)

---

## 3. 체중 기록 (Weight Record)

**Controller**: `WeightRecordController`

### 3.1. 기간별 체중 기록 조회

-   **Endpoint**: `GET /api/users/{userId}/weights`
-   **설명**: 특정 사용자의 기간별 체중 기록 목록을 조회합니다.
-   **Query Parameters**:
    -   `startDate` (String, `YYYY-MM-DD`, 필수)
    -   `endDate` (String, `YYYY-MM-DD`, 필수)
-   **Success Response** (200 OK):
    -   `data`: `List<WeightRecordResponse>`

### 3.2. 특정 날짜 체중 기록 조회

-   **Endpoint**: `GET /api/users/{userId}/weights/{date}`
-   **설명**: 특정 날짜의 체중 기록을 조회합니다.
-   **Path Variable**:
    -   `date` (String, `YYYY-MM-DD`)
-   **Success Response** (200 OK):
    -   `data`: `WeightRecordResponse`

### 3.3. 체중 기록 추가

-   **Endpoint**: `POST /api/users/{userId}/weights`
-   **설명**: 새로운 체중 기록을 추가합니다.
-   **Request Body**:
    ```json
    {
        "weight": 75.5,
        "date": "2025-12-18"
    }
    ```
-   **Success Response** (201 Created):
    -   `data`: 생성된 체중 기록의 ID (Long)

### 3.4. 체중 기록 수정

-   **Endpoint**: `PUT /api/users/{userId}/weights/{recordId}`
-   **설명**: 기존 체중 기록을 수정합니다.
-   **Request Body**:
    ```json
    {
        "weight": 74.8
    }
    ```
-   **Success Response** (200 OK):
    -   `data`: 수정된 체중 기록의 ID (Long)

### 3.5. 체중 기록 삭제

-   **Endpoint**: `DELETE /api/users/{userId}/weights/{recordId}`
-   **설명**: 특정 체중 기록을 삭제합니다.
-   **Success Response** (200 OK):
    -   `message`: "체중 기록이 삭제되었습니다."
    -   `data`: `null`

---

## 4. 식단 계획 (Meal Plan)

**Controller**: `MealPlanController`

### 4.1. 월간 식단 생성

-   **Endpoint**: `POST /api/meal-plans`
-   **설명**: 사용자의 최신 TDEE 정보를 바탕으로 1개월치 식단을 자동 생성합니다.
-   **Request Body**: (비어있음 - 인증된 사용자의 정보를 사용)
-   **Success Response** (201 Created):
    -   `data`: 생성된 식단 계획의 ID (Long)

### 4.2. 사용자의 최신 식단 조회

-   **Endpoint**: `GET /api/users/{userId}/meal-plans/latest`
-   **설명**: 해당 사용자의 가장 최근 식단 계획을 조회합니다.
-   **Success Response** (200 OK):
    -   `data`: `MealPlanResponse`

### 4.3. 특정 식단 상세 조회

-   **Endpoint**: `GET /api/meal-plans/{planId}`
-   **설명**: 특정 식단 계획의 전체 상세 정보를 조회합니다. (포함된 모든 `MealPlanDay` 정보 포함)
-   **Success Response** (200 OK):
    -   `data`: `MealPlanDetailResponse`

### 4.4. 일일 식단 상세 조회

-   **Endpoint**: `GET /api/meal-plans/days/{dayId}`
-   **설명**: 특정 날짜(`MealPlanDay`)의 상세 식단(아침, 점심, 저녁 메뉴 및 칼로리)을 조회합니다.
-   **Success Response** (200 OK):
    -   `data`: `MealPlanDayDetailResponse`

### 4.5. 식단 재료 목록 조회

-   **Endpoint**: `GET /api/meal-plans/{planId}/ingredients`
-   **설명**: 특정 식단 계획에 필요한 모든 재료를 집계하여 목록으로 반환합니다.
-   **Success Response** (200 OK):
    -   `data`: `List<IngredientResponse>` (예: `{"name": "닭가슴살", "amount": 2000, "unit": "g"}`)

---

## 5. 대시보드 (Dashboard)

**Controller**: `DashboardController`

### 5.1. 대시보드 요약 정보 조회

-   **Endpoint**: `GET /api/dashboard/summary`
-   **설명**: 대시보드에 표시될 요약 정보(예: 시작 체중, 현재 체중, 목표 체중)를 조회합니다.
-   **Success Response** (200 OK):
    -   `data`: `DashboardSummaryResponse`

### 5.2. 트렌드 데이터 조회

-   **Endpoint**: `GET /api/dashboard/trends`
-   **설명**: 기간별 체중 또는 칼로리 섭취량 추이 데이터를 조회합니다.
-   **Query Parameters**:
    -   `type` (String, `weight` 또는 `calorie`, 필수)
    -   `startDate` (String, `YYYY-MM-DD`, 필수)
    -   `endDate` (String, `YYYY-MM-DD`, 필수)
-   **Success Response** (200 OK):
    -   `data`: `DashboardTrendResponse` (라벨 목록과 데이터 목록 포함)

---

## 6. 쇼핑 (Shopping)

**Controller**: `ShoppingController`

### 6.1. 상품 검색

-   **Endpoint**: `GET /api/shopping/products`
-   **설명**: 키워드를 이용해 상품을 검색합니다.
-   **Query Parameters**:
    -   `keyword` (String, 필수)
-   **Success Response** (200 OK):
    -   `data`: `List<ShoppingProductResponse>`

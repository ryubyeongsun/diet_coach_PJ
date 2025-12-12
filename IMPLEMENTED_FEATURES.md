# Diet Coach 프로젝트 현재 진척 상황 (2025-12-12 기준)

AI가 코드를 분석하여 작성한 최신 프로젝트 현황입니다. `오늘 할일` 및 역할 분담에 참고하세요.

## 기능 구현 현황

### ✅ 구현 완료된 기능

#### 1. 사용자 관리 (User)
-   **내용**: 사용자 가입, 신체 정보(키, 몸무게, 활동량 등) 입력, 프로필 조회 기능이 모두 구현되어 있습니다.
-   **TDEE 계산**: 사용자 정보를 기반으로 기초대사량(BMR), 일일 총 에너지 소비량(TDEE), 목표 섭취 칼로리를 자동 계산하고 DB에 저장합니다.
-   **주요 API**:
    -   `POST /api/users` (사용자 생성)
    -   `GET /api/users/{id}` (사용자 프로필 조회)

#### 2. 식단 계획 (Meal Plan)
-   **내용**: 사용자 목표 칼로리에 맞춰 30일치 식단을 자동 생성하고 조회하는 핵심 기능이 구현되었습니다.
-   **세부 기능**:
    -   **식단 자동 생성**: `POST /api/meal-plans`
    -   **최신 식단 조회**: `GET /api/users/{userId}/meal-plans/latest`
    -   **특정 식단 상세 조회**: `GET /api/meal-plans/{planId}`
    -   **일일 식단 상세 조회**: `GET /api/meal-plans/days/{dayId}`
-   **프론트엔드 연동**:
    -   `MealPlanPage.vue`에서 달력 형태로 식단을 보여주고, 날짜를 클릭하면 모달로 상세 내역을 표시합니다.
    -   '식단 자동 생성' 버튼으로 새로운 식단을 만들 수 있습니다.

### 나. 백엔드
- **기술 스택**: Java 17, Spring Boot, Maven
- **데이터베이스**: MySQL, MyBatis

### 다. API 통신
- **중앙 클라이언트**: `src/api/http.js`에 `axios` 인스턴스를 중앙화하여 관리.
- **공통 처리**: 모든 API 요청/응답을 가로채는 인터셉터(Interceptor)를 구현.
    - **요청 인터셉터**: API 호출 시 콘솔에 로그를 기록하여 디버깅 편의성 증대.
    - **응답 인터셉터**: 백엔드의 공통 응답 형식(`{ success, message, data }`)을 분석하여, `success: false` 이거나 HTTP 상태 코드가 에러일 경우 예외(Exception)를 발생시켜 각 기능에서 `.catch()`로 처리할 수 있도록 표준화.

### ⏹️ 부분 구현 / 개선 필요한 기능

## 3. 구현된 기능 상세 (프론트엔드 관점)

#### 2. 식단 칼로리 업데이트 로직
-   **현황**: 식단 생성 시(`createMonthlyPlan`) 각 날짜의 총 칼로리가 계산되지만, 이 값이 데이터베이스의 `meal_plan_days` 테이블에 다시 업데이트되지는 않고 있습니다.
-   **개선 필요**: `MealPlanServiceImpl.java`에서 계산된 `totalCaloriesForDay`를 DB에 업데이트하는 로직을 추가하면 데이터 정합성을 높일 수 있습니다. (현재는 큰 문제가 아님)

### ❌ 구현되지 않은 기능

-   **일일 체중 기록**: `WeightRecord` 도메인 관련 기능 (API, DB 테이블 등)이 아직 구현되지 않았습니다.
-   **3D 캐릭터 시각화**: 기획 단계의 기능으로, 백엔드 및 프론트엔드 모두 구현이 필요합니다.

## 요약 및 결론

-   **Backend**: 사용자 관리, 식단 생성/조회 등 핵심 백엔드 기능의 뼈대는 대부분 완성되었습니다. 다만, 외부 API 연동은 Mock 상태로 실 데이터 연동 작업이 필요합니다.
-   **Frontend**: 구현된 백엔드 기능에 맞춰 식단 확인, 재료 확인 및 검색 등 주요 화면이 개발되어 있습니다. 사용자 경험(UX) 개선 및 디자인 고도화 여지는 남아있습니다.

## 4. 백엔드 API 엔드포인트 상세

이 섹션은 백엔드에서 제공하는 REST API 엔드포인트의 상세 명세입니다.

### 가. User API (`/api/users`)

- **`POST /api/users`**
    - **설명**: 신규 사용자를 생성하고, 프로필 정보와 TDEE(총 일일 에너지 소비량)를 함께 계산하여 저장합니다.
    - **Request Body**: `UserCreateRequest`
    - **Success Response**: `ApiResponse<Long>` (생성된 사용자 ID)

- **`GET /api/users/{id}`**
    - **설명**: 특정 사용자의 프로필 정보(TDEE 포함)를 조회합니다.
    - **Path Variable**: `id` (사용자 ID)
    - **Success Response**: `ApiResponse<UserProfileResponse>`

- **`GET /api/users/{id}/tdee`**
    - **설명**: 특정 사용자의 TDEE 정보만 별도로 조회합니다.
    - **Path Variable**: `id` (사용자 ID)
    - **Success Response**: `ApiResponse<TdeeResponse>`

### 나. Meal Plan API (`/api`)

- **`POST /api/meal-plans`**
    - **설명**: 특정 사용자를 위해 한 달(30일) 분량의 식단을 자동으로 생성합니다.
    - **Request Body**: `MealPlanCreateRequest` (`userId`, `startDate` 포함)
    - **Success Response**: `ApiResponse<MealPlanOverviewResponse>` (생성된 식단 전체 개요)

- **`GET /api/meal-plans/{planId}`**
    - **설명**: 특정 식단 플랜의 상세 정보를 조회합니다.
    - **Path Variable**: `planId` (식단 플랜 ID)
    - **Success Response**: `ApiResponse<MealPlanOverviewResponse>`

- **`GET /api/users/{userId}/meal-plans/latest`**
    - **설명**: 특정 사용자의 가장 최근 식단 플랜을 조회합니다.
    - **Path Variable**: `userId` (사용자 ID)
    - **Success Response**: `ApiResponse<MealPlanOverviewResponse>`

- **`GET /api/meal-plans/{planId}/ingredients`**
    - **설명**: 특정 식단 플랜에 포함된 모든 재료의 목록과 총 필요량을 조회합니다.
    - **Path Variable**: `planId` (식단 플랜 ID)
    - **Success Response**: `ApiResponse<List<MealPlanIngredientResponse>>`

- **`GET /api/users/{userId}/dashboard-summary`**
    - **설명**: 특정 사용자의 대시보드 요약 정보(오늘의 칼로리, 최근 식단 등)를 조회합니다.
    - **Path Variable**: `userId` (사용자 ID)
    - **Success Response**: `ApiResponse<DashboardSummaryResponse>`

- **`GET /api/meal-plans/days/{dayId}`**
    - **설명**: 특정 날짜(day)에 해당하는 식단의 상세 정보(아침, 점심, 저녁 메뉴 등)를 조회합니다.
    - **Path Variable**: `dayId` (식단 날짜 ID)
    - **Success Response**: `ApiResponse<MealPlanDayDetailResponse>`

### 다. Shopping API (`/api/shopping`)

- **`GET /api/shopping/search`**
    - **설명**: 키워드를 기반으로 상품을 검색합니다. (11번가 API 연동)
    - **Query Parameters**: `keyword`, `page` (옵션), `size` (옵션)
    - **Success Response**: `ApiResponse<List<ShoppingProductResponse>>`

- **`GET /api/shopping/recommendations`**
    - **설명**: 특정 식재료와 필요량을 기반으로 가장 적합한 상품을 추천합니다.
    - **Query Parameters**: `ingredient`, `neededGram` (옵션)
    - **Success Response**: `ApiResponse<List<ShoppingProductResponse>>`

### 라. Health Check API (`/api/health`)

- **`GET /api/health`**
    - **설명**: 백엔드 서버의 상태를 확인하는 헬스 체크 엔드포인트입니다.
    - **Success Response**: `ApiResponse<String>` (상태 메시지 및 "UP" 상태)

---

## 5. 실행 및 테스트 방법

1.  `frontend` 디렉토리에서 `npm install` 후 `npm run dev`로 개발 서버 실행.
2.  브라우저에서 `/meal-plans` 경로로 접속.
    - 최초 접속 시, 개발자 도구의 콘솔과 `localStorage`를 통해 신규 사용자 ID가 생성 및 저장되는 것을 확인.
    - "식단 자동 생성" 버튼을 클릭하여 식단이 생성되고 30개의 카드에 데이터가 표시되는지 확인.
3.  상단 네비게이션을 통해 `/shopping` 경로로 이동.
    - 검색창에 '닭가슴살' 등 키워드를 입력하고 검색 버튼을 클릭하여, 하단에 상품 리스트가 나타나는지 확인.

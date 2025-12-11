# 프로젝트 기능 명세서 (As-Is)

## 1. 프로젝트 개요

- **프로젝트명**: 남남코치 (TDEE 기반 식단 코치)
- **핵심 목표**: 사용자의 TDEE(총 에너지 소비량)에 맞춰 개인화된 식단을 추천하고, 관련 식료품 쇼핑을 돕는 웹 애플리케이션.
- **현재 상태**: 주요 핵심 기능(사용자 관리, 식단 생성/조회, 쇼핑 검색)의 프론트엔드-백엔드 API 연동 및 기본 UI 구현이 완료된 상태.

---

## 2. 핵심 아키텍처

### 가. 프론트엔드
- **기술 스택**: Vue 3 (Composition API, `<script setup>`) + Vite
- **주요 라이브러리**: `axios` (API 통신), `vue-router` (라우팅)
- **디렉토리 구조**:
    - `src/api`: 백엔드 API 호출 함수 모듈화
    - `src/components`: 재사용 가능한 UI 컴포넌트
    - `src/pages`: 라우팅되는 메인 페이지 컴포넌트
    - `src/router`: Vue Router 설정

### 나. 백엔드
- **기술 스택**: Java 17, Spring Boot, Maven
- **데이터베이스**: MySQL, MyBatis

### 다. API 통신
- **중앙 클라이언트**: `src/api/http.js`에 `axios` 인스턴스를 중앙화하여 관리.
- **공통 처리**: 모든 API 요청/응답을 가로채는 인터셉터(Interceptor)를 구현.
    - **요청 인터셉터**: API 호출 시 콘솔에 로그를 기록하여 디버깅 편의성 증대.
    - **응답 인터셉터**: 백엔드의 공통 응답 형식(`{ success, message, data }`)을 분석하여, `success: false` 이거나 HTTP 상태 코드가 에러일 경우 예외(Exception)를 발생시켜 각 기능에서 `.catch()`로 처리할 수 있도록 표준화.

---

## 3. 구현된 기능 상세 (프론트엔드 관점)

### 가. 사용자 관리 (핵심 기반)
- **기능**:
    1.  **테스트 사용자 자동 생성 및 유지**: 애플리케이션 첫 실행 시, `localStorage`에 `userId`가 없으면 백엔드 API를 통해 테스트용 사용자를 자동 생성.
    2.  **사용자 ID 지속성**: 생성된 `userId`를 `localStorage`에 저장하여, 브라우저를 새로고침하거나 재방문해도 동일한 사용자로 인식되도록 구현.
- **주요 파일**:
    - `pages/MealPlanPage.vue`: 사용자 초기화(`initUser`) 로직 포함.
    - `api/usersApi.js`: `createUser`, `fetchUserProfile` 등 사용자 관련 API 함수.

### 나. 식단 관리
- **기능**:
    1.  **최신 식단 조회**: 현재 사용자의 최신 식단 플랜 정보를 백엔드로부터 조회.
    2.  **식단 개요 표시**: 조회된 30일치 식단 정보를 캘린더 형태의 UI로 시각화 (`MealPlanCalendar`). 각 날짜별 총 칼로리 표시.
    3.  **식단 부재 처리**: 신규 사용자 등 식단 정보가 없는 경우, 에러가 아닌 "생성된 식단이 없습니다"라는 안내 메시지를 표시하는 UI/UX 처리.
    4.  **식단 자동 생성**: 버튼 클릭 시, 현재 날짜 기준으로 30일치 식단을 생성해달라고 백엔드에 요청하고, 성공 시 화면을 자동으로 갱신.
- **주요 파일**:
    - `pages/MealPlanPage.vue`: 식단 조회/생성/갱신 등 페이지의 모든 비즈니스 로직 담당.
    - `components/meal/MealPlanCalendar.vue`: 30일치 식단 개요를 표시하는 UI 컴포넌트.
    - `api/mealPlanApi.js`: 식단 관련 API 함수.

### 다. 쇼핑 및 재료 추천
- **기능**:
    1.  **식재료 검색**: 키워드를 입력하여 식재료를 검색 (백엔드를 통해 11번가 API와 연동).
    2.  **상품 추천**: 검색 키워드와 필요량(g)을 기반으로 최적의 추천 상품 목록을 조회.
    3.  **결과 분리 표시**: '일반 검색 결과'와 '추천 상품'을 별도의 카드 리스트로 구분하여 표시. 각 상품은 이미지, 이름, 가격, 판매처 정보 및 외부 링크 버튼을 포함.
- **주요 파일**:
    - `pages/ShoppingPage.vue`: 쇼핑 페이지의 전체 레이아웃과 검색 로직 담당.
    - `components/shopping/ShoppingSearchBar.vue`: 검색어 입력 UI.
    - `components/shopping/ShoppingProductCard.vue`: 개별 상품 정보 표시 UI.
    - `api/shoppingApi.js`: 쇼핑 관련 API 함수.

### 라. 공통 UI 컴포넌트
- **기능**: 애플리케이션 전반의 디자인 일관성을 유지하기 위한 재사용 가능한 기본 UI 컴포넌트 세트.
- **주요 파일**:
    - `components/common/NnButton.vue`
    - `components/common/NnCard.vue`
    - `components/common/NnInput.vue`

---

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

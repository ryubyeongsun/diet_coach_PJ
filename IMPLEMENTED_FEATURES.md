# 구현된 기능 목록

이 문서는 'Diet Coach' 프로젝트에서 현재 구현이 완료된 기능들을 백엔드(API)와 프론트엔드(UI)로 나누어 상세히 설명합니다. (최종 업데이트: 2025-12-15 기준)

## 1. 백엔드 (Backend Features)

### 1.1. 인증 및 보안 (Authentication & Security)
-   **Spring Security & JWT 기반 인증**: 시스템의 모든 API 요청은 JWT 토큰을 통해 인증됩니다.
-   **회원가입 API**: `POST /api/auth/signup` - 이메일, 비밀번호, 이름으로 신규 사용자를 등록합니다.
-   **로그인 API**: `POST /api/auth/login` - 로그인 성공 시, Access Token과 Refresh Token을 발급합니다.
-   **토큰 재발급 API**: Refresh Token을 이용하여 새로운 Access Token을 발급하는 로직이 구현되어 있습니다.

### 1.2. 사용자 관리 (User Management)
-   **사용자 정보 조회 API**: `GET /api/users/{id}` - 특정 사용자의 기본 정보를 조회합니다.
-   **TDEE 계산 및 조회 API**: `GET /api/users/{id}/tdee` - 사용자의 신체 정보를 바탕으로 TDEE(총 일일 에너지 소비량) 및 관련 정보를 계산하여 제공합니다. *단, 프로필 수정 기능이 없어 가입 시 정보로만 계산됩니다.*

### 1.3. 식단 관리 (Meal Plan Management)
-   **월간 식단 자동 생성 API**: `POST /api/meal-plans` - 사용자의 TDEE를 기반으로 목표 칼로리에 맞는 한 달치 식단을 자동으로 생성합니다.
-   **최신 식단 조회 API**: `GET /api/users/{userId}/meal-plans/latest` - 특정 사용자의 가장 최근에 생성된 식단 계획을 조회합니다.
-   **특정 식단 상세 조회 API**: `GET /api/meal-plans/{planId}` - ID를 기준으로 특정 식단 계획의 전체 정보를 조회합니다.
-   **일일 식단 상세 조회 API**: `GET /api/meal-plans/days/{dayId}` - 특정 날짜의 아침, 점심, 저녁 식단 메뉴와 칼로리 정보를 조회합니다.
-   **식단 재료 집계 API**: `GET /api/meal-plans/{planId}/ingredients` - 특정 식단에 필요한 모든 재료의 목록과 수량을 집계하여 반환합니다.

### 1.4. 체중 관리 (Weight Management)
-   **체중 기록 CRUD API**: 사용자의 일일 체중을 기록, 조회, 수정, 삭제하는 전체 CRUD 기능이 구현되어 있습니다.
    -   `GET, POST, PUT, DELETE /api/users/{userId}/weights/**`

### 1.5. 대시보드 (Dashboard)
-   **요약 정보 API**: `GET /api/dashboard/summary` - 대시보드에 필요한 요약 정보(예: 체중 변화)를 제공합니다.
-   **트렌드 데이터 API**: `GET /api/dashboard/trends` - 기간별 체중 및 칼로리 섭취량 변화 추이를 차트용 데이터로 제공합니다.

### 1.6. 쇼핑 (Shopping)
-   **상품 검색 API**: `GET /api/shopping/products` - 키워드를 기반으로 상품을 검색하는 기능을 제공합니다. (현재 Mock 또는 외부 API 연동)

## 2. 프론트엔드 (Frontend Features)

### 2.1. 인증 (Authentication)
-   **로그인 페이지 (`LoginPage.vue`)**: 사용자가 이메일과 비밀번호를 입력하여 로그인하고, 성공 시 JWT 토큰을 로컬 스토리지에 저장합니다.
-   **회원가입 페이지 (`SignupPage.vue`)**: 사용자가 회원가입 폼을 통해 새로운 계정을 생성합니다.
-   **전역 인증 상태 관리**: Axios 인터셉터를 설정하여 모든 API 요청 헤더에 JWT 토큰을 자동으로 추가합니다.

### 2.2. 식단 관리 (Meal Plan)
-   **식단 관리 페이지 (`MealPlanPage.vue`)**:
    -   **식단 생성**: '식단 생성' 버튼 클릭 시, 백엔드에 요청하여 한 달치 식단을 생성하고 결과를 받아옵니다.
    -   **월간 달력**: 생성된 식단을 `MealPlanCalendar` 컴포넌트를 통해 월간 달력 형태로 시각화하여 보여줍니다.
    -   **일일 상세**: 달력의 특정 날짜를 클릭하면 `MealPlanDayModal`을 통해 해당일의 상세 식단(아침/점심/저녁 메뉴, 칼로리)을 확인할 수 있습니다.
    -   **장보기 연동**: '재료 장보기' 버튼을 통해 쇼핑 페이지로 이동하는 기능이 포함되어 있습니다. (재료 목록 전달 기능은 미완성)

### 2.3. 체중 관리 (Weight)
-   **체중 관리 페이지 (`WeightPage.vue`)**:
    -   사용자가 특정 날짜의 체중을 입력, 수정, 삭제할 수 있는 UI가 구현되어 있습니다.
    -   체중 변화 추이를 `Chart.js` 기반의 차트로 시각화하여 보여줍니다.

### 2.4. 쇼핑 (Shopping)
-   **쇼핑 페이지 (`ShoppingPage.vue`)**:
    -   `ShoppingSearchBar` 컴포넌트를 통해 상품 검색 UI를 제공합니다.
    -   검색 결과를 `ShoppingProductCard` 컴포넌트의 목록으로 표시합니다.

### 2.5. 공통 컴포넌트 및 시각화
-   **재사용 UI**: `NnButton`, `NnCard`, `NnInput` 등 일관된 디자인의 공통 UI 컴포넌트가 구현되어 있습니다.
-   **데이터 시각화**: `DashboardTrendChart`, `TrendChart` 등 Chart.js를 래핑한 차트 컴포넌트를 통해 각종 트렌드 데이터를 시각화합니다.
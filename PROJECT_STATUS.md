# 프로젝트 진행 상황 보고서

## 1. 프로젝트 개요

이 프로젝트는 사용자의 TDEE(총 일일 에너지 소비량)를 기반으로 맞춤형 식단을 추천하고 관리하는 다이어트 코칭 웹 애플리케이션입니다.

- **백엔드:** Java 17, Spring Boot, MyBatis, MySQL
- **프론트엔드:** Vue.js, Vite, Vue Router, Axios, Chart.js
- **형상 관리:** Git

## 2. 현재까지 구현된 기능

### 백엔드 (API)

- **사용자 관리:**
    - `POST /api/users`: 신규 사용자 등록
    - `GET /api/users/{userId}`: 사용자 프로필 정보 조회
    - `GET /api/users/{userId}/tdee`: 사용자 TDEE 계산 및 조회

- **체중 기록:**
    - `POST /api/users/{userId}/weights`: 특정 날짜의 체중 추가 또는 수정
    - `GET /api/users/{userId}/weights?startDate&endDate`: 기간별 체중 기록 조회

- **식단 관리:**
    - `POST /api/meal-plans`: 새로운 식단 계획 생성
    - `GET /api/meal-plans/latest`: 사용자의 최신 식단 계획 조회
    - `GET /api/meal-plans/{planId}`: 특정 식단 계획 상세 조회
    - `GET /api/meal-plans/{planId}/days/{dayId}`: 특정 날짜의 식단 상세 정보 조회
    - `GET /api/meal-plans/ingredients?startDate&endDate`: 기간별 식단 재료 목록 조회

- **대시보드:**
    - `GET /api/dashboard/summary`: 대시보드 요약 정보 조회
    - `GET /api/dashboard/trend?type&startDate&endDate`: 기간별 체중 또는 칼로리 섭취 추이 조회

- **쇼핑:**
    - `GET /api/shopping/search?query`: 상품 검색
    - `GET /api/shopping/recommend?startDate&endDate`: 식단 기반 추천 상품 목록 조회

- **서버 상태:**
    - `GET /api/health`: 서버 헬스 체크

### 프론트엔드 (UI)

- **의존성 설정:**
    - `axios`: 백엔드 API 연동
    - `vue-router`: 페이지 라우팅
    - `chart.js`, `vue-chartjs`: 데이터 시각화 (차트)

- **구현된 컴포넌트 및 페이지:**
    - **라우터 설정 (`src/router/index.js`):**
        - `/`: 메인 페이지 (구현 필요)
        - `/meal-plan`: 식단 계획 페이지
        - `/weight`: 체중 관리 페이지
        - `/shopping`: 쇼핑 페이지
    - **API 모듈 (`src/api`):** 각 API(dashboard, mealPlan, shopping, users, weight)별로 기능 분리
    - **공통 컴포넌트 (`src/components/common`):**
        - `NnButton`, `NnCard`, `NnInput`: 기본 UI 컴포넌트
        - `DashboardTrendChart`: 대시보드용 차트
    - **기능별 컴포넌트 및 페이지:**
        - **식단 (`meal`):** `MealPlanCalendar`, `MealPlanDayDetail`, `MealPlanDayModal`
        - **쇼핑 (`shopping`):** `ShoppingProductCard`, `ShoppingSearchBar`
        - **대시보드 (`dashboard`):** `TrendChart`

### 데이터베이스 (Schema)

- **`users`:** 사용자 정보 (개인 정보, 목표, TDEE 등)
- **`weight_records`:** 사용자별 체중 기록
- **`meal_plans`:** 식단 계획 (기간, 목표 칼로리)
- **`meal_plan_days`:** 일일 식단 계획
- **`meal_items`:** 개별 식사 항목 (음식, 칼로리)

## 3. 종합 평가 및 향후 진행 방향

### 현재 상태

- **백엔드:** 핵심 기능 대부분이 API로 구현되어 있으며, 데이터베이스 스키마와 잘 연동됩니다. 현재 바로 사용 가능한 수준으로 보입니다.
- **프론트엔드:** 기본적인 프로젝트 구조와 라우팅, API 연동 모듈이 준비되었습니다. 식단, 쇼핑, 대시보드 등 주요 기능별로 일부 컴포넌트가 구현되었으나, 아직 전체 페이지를 완성하고 유기적으로 연결하는 작업이 필요합니다.

### 향후 역할 분담 제안

**역할 A: 프론트엔드 UI/UX 완성**

1.  **메인 대시보드 페이지 구현:**
    - `DashboardTrendChart` 와 `DashboardSummary` API를 사용하여 체중/칼로리 추이 및 요약 정보를 시각적으로 표시합니다.
    - 각 기능 페이지로 연결되는 내비게이션을 구성합니다.
2.  **사용자 등록 및 로그인 페이지 구현:**
    - `POST /api/users` API를 연동하여 회원가입 기능을 완성합니다.
    - 로그인 및 인증 상태 관리 로직을 구현합니다. (현재 백엔드에 인증/보안 기능이 명시적으로 보이지 않으므로, JWT 토큰 기반 인증 등 추가 구현이 필요할 수 있습니다.)
3.  **식단 관리 페이지 완성 (`MealPlanPage`):**
    - `MealPlanCalendar`를 중심으로 `MealPlanDayModal`을 통해 일일 식단을 확인하고 수정하는 전체 흐름을 완성합니다.
    - `POST /api/meal-plans`를 연동하여 새로운 식단 생성 기능을 구현합니다.
4.  **체중 관리 페이지 완성 (`WeightPage`):**
    - 체중 기록을 입력하고(`POST /api/users/{userId}/weights`), `Chart.js`를 이용해 시각적으로 조회하는 기능을 완성합니다.
5.  **쇼핑 페이지 완성 (`ShoppingPage`):**
    - `ShoppingSearchBar`로 상품을 검색하고, `ShoppingProductCard`를 통해 검색 및 추천 상품 목록을 표시하는 기능을 완성합니다.

**역할 B: 백엔드 고도화 및 테스트**

1.  **인증 및 보안 강화:**
    - Spring Security 와 JWT(JSON Web Token)를 도입하여 로그인, 회원가P, API 접근 제어 등 보안 기능을 구현합니다.
    - 사용자 비밀번호 암호화를 적용합니다.
2.  **API 테스트 및 검증:**
    - Postman 또는 JUnit/Mockito 등을 사용하여 현재 구현된 모든 API의 정합성 및 예외 처리를 테스트하고 문서를 보완합니다.
    - 특히 식단 생성(`POST /api/meal-plans`)과 같이 복잡한 로직을 가진 API에 대한 테스트 케이스를 강화합니다.
3.  **외부 API 연동 안정화 (`ShoppingController`):**
    - 현재 `RestTemplate` 또는 `WebClient` 등으로 구현되었을 쇼핑 API 클라이언트의 예외 처리(타임아웃, 4xx/5xx 에러 등)를 강화하고, 필요시 Resilience4j 등을 이용한 Circuit Breaker 패턴 도입을 검토합니다.
4.  **배포 환경 구축:**
    - `mvnw`를 사용하여 프로젝트를 빌드하고, Dockerfile을 작성하여 애플리케이션을 컨테이너화하는 작업을 준비합니다.
    - AWS, Google Cloud 등 클라우드 환경에 배포하는 스크립트를 작성합니다.
5.  **데이터베이스 마이그레이션:**
    - Flyway 또는 Liquibase 와 같은 데이터베이스 마이그레이션 툴을 도입하여 `schema.sql` 변경 이력을 관리하고, 자동화를 준비합니다.

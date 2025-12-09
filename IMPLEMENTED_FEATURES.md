# Diet Coach 프로젝트 - 현재까지 구현된 기능 명세

이 문서는 Diet Coach 프로젝트의 현재 개발 현황을 공유하고, 페어 프로그래밍 역할 분담을 돕기 위해 작성되었습니다.

## 1. 기술 스택

- **Backend**: Java 17, Spring Boot 3.x, MyBatis, MySQL
- **Frontend**: Vue.js 3, Vite
- **API Client**: 11번가 Open API (쇼핑 정보)

---

## 2. Backend 구현 기능 (Java / Spring Boot)

### 2.1. 사용자 관리 (User Management)

사용자 정보 등록 및 조회를 담당합니다.

- **주요 파일**:
    - `UserController.java`
    - `UserService.java`, `UserServiceImpl.java`
    - `UserMapper.java`, `UserMapper.xml`
    - `TdeeCalculator.java`
- **API Endpoints**:
    - `POST /api/users`
        - **설명**: 신규 사용자를 등록합니다. 요청 시 사용자의 기본 정보(성별, 나이, 키, 체중 등)를 받아 TDEE(총 에너지 소비량)를 계산하고 함께 저장합니다.
        - **요청**: `UserCreateRequest.java`
        - **응답**: 생성된 `userId`
    - `GET /api/users/{id}`
        - **설명**: 특정 사용자의 프로필 정보(TDEE 포함)를 조회합니다.
        - **응답**: `UserProfileResponse.java`
    - `GET /api/users/{id}/tdee`
        - **설명**: 특정 사용자의 TDEE 정보만 별도로 조회합니다.
        - **응답**: `TdeeResponse.java`

### 2.2. 식단 관리 (Meal Plan Management)

사용자별 식단을 생성하고 조회하는 기능을 담당합니다.

- **주요 파일**:
    - `MealPlanController.java`
    - `MealPlanService.java`, `MealPlanServiceImpl.java`
    - `MealPlanMapper.java`, `MealPlanMapper.xml`
- **API Endpoints**:
    - `POST /api/meal-plans`
        - **설명**: 특정 사용자를 위한 한 달치 식단을 **자동 생성**합니다. `userId`와 시작 날짜를 받아, 해당 유저의 TDEE를 기반으로 식단을 구성합니다.
        - **요청**: `MealPlanCreateRequest.java`
        - **응답**: 생성된 식단 전체 개요 (`MealPlanOverviewResponse.java`)
    - `GET /api/meal-plans/{planId}`
        - **설명**: 특정 식단 ID에 해당하는 식단 전체 정보를 조회합니다.
        - **응답**: `MealPlanOverviewResponse.java`
    - `GET /api/users/{userId}/meal-plans/latest`
        - **설명**: 특정 사용자의 가장 최근에 생성된 식단 정보를 조회합니다.
        - **응답**: `MealPlanOverviewResponse.java`

### 2.3. 쇼핑 정보 (Shopping Information)

식단에 필요한 재료를 외부 API를 통해 검색하고 추천합니다.

- **주요 파일**:
    - `ShoppingController.java`
    - `ShoppingService.java`, `ShoppingServiceImpl.java`
    - `ElevenstShoppingClient.java` (11번가 API 연동)
- **API Endpoints**:
    - `GET /api/shopping/search`
        - **설명**: 키워드를 기반으로 11번가 상품을 검색합니다. (페이징 가능)
        - **파라미터**: `keyword`, `page` (선택), `size` (선택)
        - **응답**: `List<ShoppingProductResponse>`
    - `GET /api/shopping/recommendations`
        - **설명**: 특정 재료명과 필요 그램(g)을 기반으로 적절한 상품을 추천합니다.
        - **파라미터**: `ingredient`, `neededGram` (선택)
        - **응답**: `List<ShoppingProductResponse>`

---

## 3. Frontend 구현 기능 (Vue.js)

### 3.1. 화면 라우팅

- **주요 파일**: `router/index.js`
- **구현 내용**:
    - `/meal-plans`: 식단 계획 페이지 (`MealPlanPage.vue`)
    - `/shopping`: 쇼핑 페이지 (`ShoppingPage.vue`)
    - `/`: 기본 경로, 현재 식단 계획 페이지로 연결

### 3.2. 쇼핑 페이지 (`/shopping`)

- **주요 파일**: `pages/ShoppingPage.vue`, `api/shopping.js`
- **구현 내용**:
    - **기능**: 백엔드의 쇼핑 API와 연동하여 실제 데이터를 화면에 렌더링합니다.
    - **상세**:
        - 검색 바를 통해 키워드를 입력하면 `GET /api/shopping/search` API를 호출하여 상품 목록을 표시합니다.
        - 동일 키워드로 `GET /api/shopping/recommendations` API를 호출하여 추천 상품 목록을 별도 카드에 표시합니다.
        - 로딩 및 에러 상태 처리가 구현되어 있습니다.

### 3.3. 식단 계획 페이지 (`/meal-plans`)

- **주요 파일**: `pages/MealPlanPage.vue`
- **구현 내용**:
    - **기능**: 현재 **UI 목업(Mockup) 상태**이며, 실제 데이터 연동은 되어있지 않습니다.
    - **상세**:
        - 한 달 식단 통계(목표 달성률, 평균 섭취 칼로리 등)와 월별 식단 개요를 보여주는 복잡한 레이아웃이 디자인되어 있습니다.
        - 현재는 컴포넌트 내에 하드코딩된 더미 데이터(`exampleDays`)를 사용해 화면을 그리고 있습니다.
        - "식단 자동 생성" 버튼이 있으나, `POST /api/meal-plans` API와의 연동 기능은 아직 구현되지 않았습니다.

### 3.4. 공통 컴포넌트

- **위치**: `components/common/`
- **구현 내용**:
    - `NnButton.vue`: 기본 버튼
    - `NnCard.vue`: UI 섹션을 나누는 카드 컴포넌트
    - `NnInput.vue`: 기본 입력 필드

---

## 4. 요약 및 제언

- **Backend**: 사용자, 식단, 쇼핑의 핵심 CRUD 및 비즈니스 로직 API가 대부분 구현되어 있습니다.
- **Frontend**: 쇼핑 페이지는 API 연동까지 완료되었으나, 핵심 기능인 **식단 계획 페이지는 UI만 구현된 상태**로 백엔드와의 데이터 연동 작업이 필요합니다.

역할 분담 시, 한 명은 프론트엔드의 식단 페이지 API 연동 및 고도화 작업을, 다른 한 명은 백엔드의 미진한 부분(예: 인증, 테스트 코드, 상세 예외 처리)을 보완하는 방향으로 진행하면 효율적일 것입니다.

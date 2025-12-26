# Diet Coach 프로젝트 - 현재 진행 상황 분석 (2025-12-15)

## 1. 개요

본 문서는 'Diet Coach' 프로젝트의 현재 개발 상황을 코드 기반으로 분석하여 정리한 보고서입니다. TDEE(총 에너지 소비량) 기반의 식단 추천 및 관리 애플리케이션으로, Spring Boot 백엔드와 Vue.js 프론트엔드로 구성되어 있습니다.

현재 사용자 인증, 식단 생성 및 조회, 체중 기록, 대시보드 등 핵심 기능의 기반이 구현된 상태입니다.

## 2. 기술 스택

### 백엔드 (Backend)
-   **언어/프레임워크**: Java 17, Spring Boot 3.5.8
-   **인증**: Spring Security, JJWT (JWT 토큰 기반)
-   **데이터베이스**: MySQL, MyBatis (ORM/Mapper)
-   **API**: Spring Web (RESTful API)
-   **기타**: Lombok

### 프론트엔드 (Frontend)
-   **프레임워크**: Vue.js 3
-   **빌드 도구**: Vite
-   **라우팅**: Vue Router
-   **API 통신**: Axios
-   **차트/시각화**: Chart.js, vue-chartjs
-   **UI**: 컴포넌트 기반 UI (버튼, 카드 등 재사용 가능한 컴포넌트 다수)

## 3. 기능별 진행 상황

### 백엔드 (API)

-   **인증 (`AuthController`)**:
    -   `POST /api/auth/signup`: 회원가입 (최근 `email`, `password`, `name`만 받도록 수정 완료)
    -   `POST /api/auth/login`: 로그인 (JWT 토큰 발급)
-   **사용자 (`UserController`)**:
    -   `GET /api/users/{id}`: 사용자 정보 조회
    -   `GET /api/users/{id}/tdee`: TDEE 계산
-   **체중 기록 (`WeightRecordController`)**:
    -   `GET, POST, PUT, DELETE /api/users/{userId}/weights/**`: 사용자의 체중 기록에 대한 전체 CRUD 기능 구현 완료.
-   **식단 (`MealPlanController`)**:
    -   `POST /api/meal-plans`: TDEE 기반 월간 식단 자동 생성 (핵심 로직)
    -   `GET /api/meal-plans/{planId}`: 특정 식단 상세 조회
    -   `GET /api/users/{userId}/meal-plans/latest`: 사용자의 최신 식단 조회
    -   `GET /api/meal-plans/{planId}/ingredients`: 식단에 필요한 모든 재료 목록 집계
    -   `GET /api/meal-plans/days/{dayId}`: 특정 날짜의 식단 상세 조회 (아침/점심/저녁)
-   **대시보드 (`DashboardController`)**:
    -   `GET /api/dashboard/summary`: 체중 변화 등 요약 정보 제공
    -   `GET /api/dashboard/trends`: 기간별 체중, 칼로리 섭취 트렌드 데이터 제공
-   **쇼핑 (`ShoppingController`)**:
    -   `GET /api/shopping/products`: 상품 검색 기능 (외부 API 연동으로 추정)

### 프론트엔드 (UI/Page)

-   **`LoginPage.vue`, `SignupPage.vue`**:
    -   인증 기능과 직접적으로 연동되는 페이지. API 연동 및 UI 구현 완료.
-   **`WeightPage.vue`**:
    -   체중 CRUD를 위한 UI. 차트와 함께 기록을 관리하는 기능 구현.
-   **`MealPlanPage.vue`**:
    -   **현재 가장 고도화된 페이지.**
    -   식단 자동 생성 요청 기능 구현.
    -   서버에서 받은 데이터를 바탕으로 월간 `MealPlanCalendar` 컴포넌트에 렌더링.
    -   체중, 칼로리 섭취량 `TrendChart` 시각화.
    -   '재료 장보기' 버튼을 통해 쇼핑 페이지로 연동하는 기능 포함.
    -   날짜 클릭 시 `MealPlanDayModal`을 통해 상세 식단 조회.
-   **`ShoppingPage.vue`**:
    -   상품 검색 UI 및 결과 표시 기능 구현.
    -   `MealPlanPage`에서 `planId`를 받아 재료 목록을 표시하는 기능은 아직 미완성으로 보임.

## 4. 종합 분석 및 역할 분담 제언

### 분석 요약
-   **완성도**: 프로젝트의 핵심 MVP(Minimum Viable Product) 기능인 **'사용자 가입 -> TDEE 계산 -> 식단 생성 -> 식단 조회'** 의 큰 흐름이 백엔드와 프론트엔드에 걸쳐 구현되어 있습니다.
-   **강점**: 식단 생성 및 조회(`MealPlan`) 기능은 단순 CRUD를 넘어 비즈니스 로직과 UI/UX가 상당히 구체화되어 있습니다. 컴포넌트 기반 아키텍처가 잘 잡혀있어 확장성이 용이합니다.
-   **보완점**:
    1.  **사용자 프로필 수정**: 회원가입 시 받지 않기로 한 `성별`, `키`, `활동량` 등의 정보를 사용자가 **로그인 후 입력/수정**할 수 있는 기능이 필요합니다. (현재 TDEE 계산 및 식단 생성에 필수적)
    2.  **식단-쇼핑 연계**: `MealPlanPage`에서 '장보기' 버튼을 누르면 `ShoppingPage`로 이동하지만, 해당 식단(`planId`)의 재료 목록을 실제로 보여주는 기능 구현이 필요합니다.

### 역할 분담 제언

이를 바탕으로 다음과 같이 역할을 분담하여 프로젝트를 효율적으로 진행할 수 있습니다.

**[담당자 A: 신규 핵심 기능 개발]**
-   **주요 목표**: '사용자 프로필 관리' 기능 완성
-   **작업 내용**:
    1.  **백엔드**:
        -   `PUT /api/users/{id}/profile` 과 같은 사용자 프로필(키, 성별, 활동량 등) 업데이트 API 엔드포인트 구현.
        -   관련 `UserService` 로직 및 `UserMapper` 쿼리 작성.
    2.  **프론트엔드**:
        -   '마이페이지' 또는 '프로필 수정' 페이지(`ProfilePage.vue`) 신규 생성.
        -   사용자 정보를 조회하고 수정 요청을 보내는 폼 UI 및 API 연동 로직 구현.
        -   (선택) 로그인 후 프로필 정보가 없는 사용자에게 프로필 입력을 유도하는 UI/UX 추가.

**[담당자 B: 기존 기능 고도화 및 연계]**
-   **주요 목표**: '식단-쇼핑' 기능 연계 및 대시보드 강화
-   **작업 내용**:
    1.  **백엔드**:
        -   `ShoppingController` 또는 `MealPlanController`에 `planId`를 받아 해당 식단의 재료 목록을 반환하는 API가 이미 있으므로(`GET /api/meal-plans/{planId}/ingredients`), 필요시 응답 데이터 포맷을 프론트엔드와 협의하여 조정.
    2.  **프론트엔드**:
        -   `ShoppingPage.vue`를 수정하여, `planId` 쿼리 스트링이 있을 경우 해당 식단의 재료 목록을 API로 조회하여 표시하는 기능 구현.
        -   검색 기능과 재료 목록 표시 기능을 명확히 분리하여 UX 개선.
        -   `MealPlanPage` 또는 별도의 대시보드 페이지의 시각화/통계 데이터 표시 기능 강화.
# Diet Coach 프로젝트 현황 분석

## 1. 개요
이 문서는 Diet Coach 프로젝트의 현재 기술 스택, 아키텍처, 구현 현황을 분석하여 팀원 간의 역할 분담 및 향후 개발 방향 설정을 돕기 위해 작성되었습니다.

## 2. 기술 스택 (Technology Stack)
- **Backend**: Java 17, Spring Boot 3.5.8, Spring Security, MyBatis 3.0.5, MySQL, JJWT (JSON Web Token)
- **Frontend**: Vue.js 3.5.24, Vite, Vue Router, Axios
- **Build Tool**: Maven

## 3. 백엔드 분석 (Backend Analysis)

### 3.1. 패키지 구조
최근 리패키징을 통해 기능 중심으로 구조가 명확하게 정리되었습니다.

```
src/main/java/com/dietcoach/project
├── DietCoachApplication.java     // Spring Boot 시작점
├── client                        // 외부 API 클라이언트
│   └── shopping
├── common                        // 공통 유틸리티 및 예외 처리
│   ├── ApiResponse.java
│   ├── TdeeCalculator.java
│   └── error
├── config                        // Spring 설정 (Security, CORS, Web 등)
│   ├── CorsConfig.java
│   ├── HttpClientConfig.java
│   ├── RestClientConfig.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller                    // API 엔드포인트 정의
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── HealthController.java
│   ├── MealPlanController.java
│   ├── ShoppingController.java
│   ├── UserController.java
│   └── WeightRecordController.java
├── domain                        // 핵심 도메인 객체 (Entity)
│   ├── ActivityLevel.java
│   ├── Gender.java
│   ├── GoalType.java
│   ├── ShoppingProduct.java
│   ├── User.java
│   ├── WeightRecord.java
│   └── meal
├── dto                           // 데이터 전송 객체
│   ├── ShoppingProductResponse.java
│   ├── ...
│   └── ...
├── mapper                        // MyBatis 매퍼 인터페이스
├── security                      // Spring Security 관련 클래스 (JWT 등)
└── service                       // 비즈니스 로직 구현
```

### 3.2. 주요 기능 구현 현황
- **인증 (Authentication)**: `AuthController`와 `security` 패키지를 통해 JWT 기반의 회원가입 및 로그인 기능의 기본 골격이 구현되었습니다.
- **사용자 관리 (User Management)**: `UserController`를 통해 사용자 정보 조회, 초기 설정(키, 몸무게, 활동량 등) 기능이 구현되었습니다. TDEE 계산 로직(`TdeeCalculator`)이 포함되어 있습니다.
- **식단 관리 (Meal Plan)**: `MealPlanController`를 중심으로 식단 생성, 조회, 수정, 삭제(CRUD) 기능의 API가 정의되어 있습니다.
- **체중 기록 (Weight Record)**: `WeightRecordController`를 통해 체중 기록 및 조회가 가능합니다.
- **대시보드 (Dashboard)**: `DashboardController`에서 체중 변화 추이 등 대시보드에 필요한 데이터를 제공하는 API가 구현 중입니다.
- **쇼핑 (Shopping)**: `ShoppingController`와 `client.shopping` 패키지를 통해 외부 쇼핑 API(11번가)를 연동하여 상품을 검색하는 기능이 구현되어 있습니다. 특히 `ShoppingClient` 인터페이스를 두어 실제 API 호출(`ElevenstShoppingClient`)과 모의 객체(`MockShoppingClient`)를 분리한 점은 주목할 만합니다.

### 3.3. 데이터베이스
- MyBatis를 ORM으로 사용하며, SQL 쿼리는 `src/main/resources/mapper/` 디렉토리의 XML 파일(`UserMapper.xml`, `MealPlanMapper.xml` 등)에 작성되어 있습니다.
- `schema.sql`을 통해 초기 테이블 구조를 관리합니다.

## 4. 프론트엔드 분석 (Frontend Analysis)

### 4.1. 디렉토리 구조
- **`src/api`**: 백엔드 API와 통신하는 `axios` 인스턴스 및 각 기능별 API 호출 함수가 정의되어 있습니다. (`authApi.js`, `mealPlanApi.js` 등)
- **`src/pages`**: 각 화면을 구성하는 주된 Vue 컴포넌트가 위치합니다. (`LoginPage.vue`, `MealPlanPage.vue`, `ShoppingPage.vue` 등)
- **`src/components`**: 페이지 전반에서 사용되는 공용 컴포넌트(`common`)와 특정 기능에 종속된 컴포넌트(`meal`, `dashboard`)가 분리되어 있습니다.
- **`src/router`**: `vue-router`를 사용한 페이지 간 라우팅 설정이 정의되어 있습니다.

### 4.2. 주요 기능 및 페이지
- 대부분의 백엔드 기능에 대응하는 프론트엔드 페이지(`LoginPage`, `SignupPage`, `MealPlanPage`, `ShoppingPage`, `WeightPage` 등)가 생성되어 기본 라우팅이 완료된 상태입니다.
- API 연동을 위한 `api` 모듈들이 준비되어 있어, 각 페이지에서 백엔드 데이터를 받아와 화면에 렌더링하는 작업이 진행 중이거나 진행될 예정입니다.
- `Chart.js` 의존성이 포함되어 있어, 대시보드의 데이터 시각화 기능 구현이 예정되어 있음을 알 수 있습니다.

## 5. 전체 진행 상황 요약
- **완료된 부분**:
  - 프로젝트의 전체적인 기술 스택 선정 및 설정 완료.
  - 백엔드의 기능 중심 패키지 구조 확립.
  - 사용자 인증, 회원 정보 관리, 식단, 체중, 쇼핑 등 핵심 기능의 기본 API 엔드포인트 정의 및 기본 로직 구현.
  - 프론트엔드의 페이지 라우팅 및 기본 컴포넌트 구조 설정 완료.
- **진행 중/필요한 부분**:
  - 각 기능별 세부 비즈니스 로직 고도화 (예외 처리, 검증 등).
  - 프론트엔드 페이지에서 API 연동을 완료하고, 사용자 인터페이스(UI)를 상세하게 구현.
  - 대시보드 데이터 시각화 기능 구현.
  - Spring Security와 JWT 관련 설정 상세 마무리 및 권한 제어 적용.
  - 전체 기능에 대한 통합 테스트 및 단위 테스트 코드 작성.

이 문서를 바탕으로 각 기능의 담당자를 지정하고, 필요한 세부 작업들을 나누어 진행하는 것이 효율적일 것으로 판단됩니다.

# 패키지 및 디렉터리 구조 상세

이 문서는 백엔드(Java/Spring Boot)와 프론트엔드(Vue.js)의 상세한 패키지 및 디렉터리 구조를 설명합니다.

## 1. 백엔드 패키지 구조 (`src/main/java/com/dietcoach/project`)

백엔드는 기능과 역할에 따라 명확하게 패키지를 분리하여 관리합니다.

```
com.dietcoach.project
├── DietCoachApplication.java       # Spring Boot 애플리케이션의 메인 클래스
│
├── client                          # 외부 API 연동 클라이언트
│   └── shopping
│       └── ElevenstShoppingClient
│
├── common                          # 프로젝트 전역 공통 유틸리티
│   ├── ApiResponse.java              # 공통 API 응답 래퍼 클래스
│   ├── TdeeCalculator.java         # TDEE 계산 유틸리티
│   └── error                       # 전역 예외 처리 관련 클래스
│
├── config                          # 애플리케이션 설정
│   ├── CorsConfig.java               # CORS (Cross-Origin Resource Sharing) 설정
│   ├── HttpClientConfig.java       # HTTP 클라이언트 (RestTemplate/WebClient) 빈 설정
│   ├── RestClientConfig.java
│   ├── SecurityConfig.java           # Spring Security 설정
│   └── WebConfig.java                # Web MVC 관련 설정 (Interceptor 등)
│
├── controller                      # API 엔드포인트 정의 (HTTP 요청/응답 처리)
│   ├── AuthController.java           # 인증/인가 (회원가입, 로그인)
│   ├── DashboardController.java
│   ├── HealthController.java
│   ├── MealPlanController.java
│   ├── ShoppingController.java
│   ├── UserController.java
│   └── WeightRecordController.java
│
├── domain                          # 데이터베이스 테이블과 1:1 매핑되는 핵심 엔티티
│   ├── ActivityLevel.java            # Enum
│   ├── Gender.java                   # Enum
│   ├── GoalType.java                 # Enum
│   ├── MealItem.java
│   ├── MealPlan.java
│   ├── MealPlanDay.java
│   ├── ShoppingProduct.java
│   ├── User.java
│   ├── WeightRecord.java
│   └── auth
│       └── RefreshToken.java
│
├── dto                             # 데이터 전송 객체 (Request/Response)
│   ├── auth/
│   ├── user/
│   └── ... (기타 기능별 DTO)
│
├── mapper                          # MyBatis Mapper 인터페이스 (DAO)
│   ├── UserMapper.java
│   ├── WeightRecordMapper.java
│   └── ... (기타 기능별 Mapper)
│
├── security                        # 인증/보안 관련 핵심 로직
│   ├── jwt/                          # JWT 토큰 생성, 검증, 파싱 관련 클래스
│   ├── service/                      # UserDetailsService 구현체
│   └── handler/                      # 로그인/로그아웃 핸들러
│
└── service                         # 비즈니스 로직 구현
    ├── impl/                         # Service 인터페이스의 구현체
    │   └── UserServiceImpl.java
    └── UserService.java              # Service 인터페이스
```

## 2. 프론트엔드 디렉터리 구조 (`frontend/src`)

프론트엔드는 기능(feature)과 역할(role)을 기준으로 디렉터리를 구성합니다.

```
src/
├── App.vue                         # 최상위 루트 컴포넌트
├── main.js                         # 애플리케이션 진입점 (Vue 인스턴스 생성)
│
├── api/                            # 백엔드 API 연동 모듈
│   ├── authApi.js
│   ├── dashboardApi.js
│   ├── http.js                       # Axios 인스턴스 설정 및 인터셉터
│   ├── mealPlanApi.js
│   ├── shoppingApi.js
│   ├── usersApi.js
│   └── weightApi.js
│
├── assets/                         # 정적 에셋 (이미지, CSS 등)
│   └── vue.svg
│
├── components/                     # 재사용 가능한 UI 컴포넌트
│   ├── common/                       # 애플리케이션 전역 공통 컴포넌트
│   │   ├── DashboardTrendChart.vue
│   │   ├── NnButton.vue
│   │   ├── NnCard.vue
│   │   └── NnInput.vue
│   ├── dashboard/                    # 대시보드 관련 컴포넌트
│   ├── meal/                         # 식단 관리 관련 컴포넌트
│   └── shopping/                     # 쇼핑 관련 컴포넌트
│
├── pages/                          # 라우터에 매핑되는 페이지 단위 컴포넌트
│   ├── CartPage.vue
│   ├── LoginPage.vue
│   ├── MealPlanPage.vue
│   ├── ProfileSetupPage.vue
│   ├── ShoppingPage.vue
│   ├── SignupPage.vue
│   └── WeightPage.vue
│
├── router/                         # Vue Router 설정
│   └── index.js                      # 라우트 정의
│
└── utils/                          # 전역 유틸리티 함수
    ├── auth.js                       # 인증 토큰(JWT) 저장/조회/삭제
    └── globalState.js                # 간단한 전역 상태 관리 (필요시)
```

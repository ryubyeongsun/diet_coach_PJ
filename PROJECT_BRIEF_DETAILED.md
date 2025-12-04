# Diet Coach 상세 프로젝트 브리핑 (AI 개발자용)

**버전: 2.0**
**마지막 업데이트: 2025-12-02**

이 문서는 AI 개발자가 프로젝트의 맥락을 완벽하게 이해하고 즉시 개발에 참여할 수 있도록 모든 핵심 정보를 제공합니다.

---

## 1. 프로젝트 개요

**Diet Coach**는 사용자의 신체 정보와 활동 수준을 바탕으로 TDEE(총 일일 에너지 소비량)를 계산하고, 이를 기반으로 다이어트 목표 설정, 식단 추천, 관련 상품 쇼핑 기능을 제공하는 Spring Boot 기반 웹 애플리케이션입니다.

- **핵심 목표**: 사용자 맞춤형 건강 관리 및 다이어트 보조
- **주요 기능**:
    - **구현 완료**:
        - 사용자 정보(신체, 목표 등) 등록 및 조회
        - 기초대사량(BMR), 총 일일 에너지 소비량(TDEE), 목표 섭취 칼로리 계산
        - 외부 쇼핑몰 API(Mock)와 연동하여 키워드 기반 상품 검색 및 추천
    - **구현 예정**:
        - 월간 식단 생성 및 관리
        - 일일 체중 기록 및 변화 추적
        - 3D 캐릭터를 통한 체중 변화 시각화

---

## 2. 실행 환경 및 로컬 개발 가이드

### 2.1. 기술 스택
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.5.8
- **데이터베이스 연동**: MyBatis
- **데이터베이스**: MySQL
- **빌드 도구**: Maven
- **개발 환경**: STS 또는 Eclipse 권장

### 2.2. 로컬 서버 실행 방법
1.  **데이터베이스 생성**: MySQL에 `dietcoach` 스키마를 생성합니다.
    ```sql
    CREATE DATABASE dietcoach;
    ```
2.  **`application.properties` 설정**: `src/main/resources/application.properties` 파일을 아래 예시와 같이 수정합니다.
    ```properties
    server.port=8080

    # Database Settings
    spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
    spring.datasource.url=jdbc:mysql://localhost:3306/dietcoach?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    spring.datasource.username=YOUR_DB_USER
    spring.datasource.password=YOUR_DB_PASSWORD

    # MyBatis Settings
    mybatis.mapper-locations=classpath:mapper/*.xml
    mybatis.type-aliases-package=com.dietcoach.project.domain
    mybatis.configuration.map-underscore-to-camel-case=true

    # Logging
    logging.level.com.dietcoach.project=DEBUG

    # External API Keys
    elevenst.api.key=YOUR_ELEVENST_API_KEY
    ```
3.  **서버 실행**: IDE(STS/Eclipse)에서 `DietCoachApplication.java` 파일을 우클릭하여 `Run As > Spring Boot App`으로 실행합니다.
4.  **헬스 체크**: 브라우저나 API 테스트 도구로 `http://localhost:8080/api/health`를 호출하여 서버가 정상적으로 실행되었는지 확인합니다.

---

## 3. AI 협업 가이드라인

> **매우 중요: 작업을 시작하기 전에 반드시 이 가이드를 읽고 준수하세요.**

### 3.1. 핵심 원칙
1.  **가정 금지, 코드 확인 필수**: 이 문서에 없는 정보(`TdeeCalculator.java` 등)가 필요하면, **절대 코드를 추측하지 마세요.** 대신 사용자에게 명시적으로 요청하세요.
    > **요청 예시**: "작업을 계속하려면 `[파일 경로]` 파일의 내용이 필요합니다. 코드를 보여주시겠어요?"
2.  **계층형 아키텍처 유지**:
    - **Controller**: 클라이언트의 요청/응답 처리, DTO 변환 및 유효성 검증만 담당합니다. Service 레이어만 호출합니다.
    - **Service**: 비즈니스 로직, 계산, 트랜잭션을 담당합니다. Domain 객체를 처리합니다.
    - **Mapper(MyBatis)**: 데이터베이스 접근(SQL 실행)만 담당합니다.
3.  **DTO 사용 원칙**: **Domain 엔티티를 Controller 응답으로 절대 직접 노출하지 마세요.** 항상 응답용 DTO(`...Response`)로 변환하여 반환합니다.
4.  **공통 응답 규칙**: 모든 REST API 응답은 `common/ApiResponse<T>` 래퍼 클래스로 감싸서 일관된 형식을 유지합니다.
5.  **일관성 유지**: 문서의 스키마/네이밍과 실제 코드가 다르다면, **반드시 한쪽을 기준으로 통일**해야 합니다. 사용자에게 어느 쪽을 기준으로 할지 확인받으세요.

### 3.2. 명명 규칙
#### 클래스 (PascalCase)
| 역할 | 규칙 | 예시 |
| --- | --- | --- |
| Controller | `...Controller` | `UserController` |
| Service (구현체) | `...ServiceImpl` | `UserServiceImpl` |
| Entity (Domain) | 역할에 맞는 명사 | `User`, `ShoppingProduct` |
| DTO (Request) | `...CreateRequest`, `...UpdateRequest` | `UserCreateRequest` |
| DTO (Response) | `...Response` | `UserProfileResponse` |
| Exception | `...Exception` | `BusinessException` |
| Enum | 역할에 맞는 명사 | `Gender`, `ActivityLevel` |

#### 메소드 (camelCase)
| 목적 | 접두사 | 예시 |
| --- | --- | --- |
| 조회 (단일) | `get...`, `find...` | `getUserProfile`, `findById` |
| 조회 (목록) | `get...List`, `findAll...` | `getShoppingProducts` |
| 생성 | `create...` | `createUser` |
| 수정 | `update...` | `updateUser` |
| 삭제 | `delete...` | `deleteUser` |
| 계산 | `calculate...` | `calculateTdee` |

---

## 4. 데이터베이스 스키마 및 변수 규칙

### 4.1. `users` 테이블 스키마
- **파일 위치**: `src/main/resources/schema/schema.sql`
```sql
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    gender           VARCHAR(10)  NOT NULL,      -- MALE / FEMALE / OTHER
    birth_date       DATE         NOT NULL,
    height           DOUBLE       NOT NULL,      -- cm
    weight           DOUBLE       NOT NULL,      -- kg
    activity_level   VARCHAR(20)  NOT NULL,      -- SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE
    goal_type        VARCHAR(20)  NOT NULL,      -- LOSE_WEIGHT / MAINTAIN / GAIN_WEIGHT
    bmr              DOUBLE       NULL,
    tdee             DOUBLE       NULL,
    target_calories  DOUBLE       NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.2. 주요 변수 명명 규칙 (Java: camelCase, DB: snake_case)
| 변수명 (Java) | 컬럼명 (DB) | 타입 예시 | 설명 |
| --- | --- | --- | --- |
| `id` | `id` | Long | 데이터베이스 Primary Key |
| `email` | `email` | String | 사용자 이메일 (로그인 ID) |
| `password` | `password` | String | 사용자 비밀번호 |
| `name` | `name` | String | 사용자 이름 |
| `gender` | `gender` | Gender | 성별 (Enum) |
| `birthDate` | `birth_date` | LocalDate | 생년월일 |
| `height` | `height` | Double | 키 (cm) |
| `weight` | `weight` | Double | 몸무게 (kg) |
| `activityLevel` | `activity_level` | ActivityLevel | 활동 수준 (Enum) |
| `goalType` | `goal_type` | GoalType | 목표 유형 (Enum) |
| `bmr` | `bmr` | Double | 기초대사량 |
| `tdee` | `tdee` | Double | 총 에너지 소비량 |
| `targetCalories`| `target_calories` | Double | 목표 섭취 칼로리 |
| `createdAt` | `created_at` | LocalDateTime | 생성 일시 |
| `updatedAt` | `updated_at` | LocalDateTime | 수정 일시 |
| `keyword` | - | String | 검색 키워드 |
| `page` | - | Integer | 페이지 번호 |
| `size` | - | Integer | 페이지 당 아이템 수 |
| `ingredient` | - | String | 식재료 이름 |
| `neededGram` | - | Integer | 필요한 그램(g) 수 |
| `productName` | - | String | 상품명 |
| `price` | - | Integer | 상품 가격 |
| `imageUrl` | - | String | 상품 이미지 URL |
| `productUrl` | - | String | 상품 상세 페이지 URL |

---

## 5. 도메인별 상세 명세

### 5.1. User 도메인 (구현 완료)
- **엔티티**: `User.java`
- **API**:
    - `POST /api/users`: 신규 사용자 생성. 신체 정보로 TDEE를 계산하여 함께 저장.
    - `GET /api/users/{id}`: 특정 사용자의 전체 프로필 정보 조회.
    - `GET /api/users/{id}/tdee`: 특정 사용자의 TDEE 관련 정보만 조회.
- **핵심 로직**: `UserServiceImpl`에서 TDEE 계산 및 사용자 정보 저장을 담당.

### 5.2. Shopping 도메인 (구현 완료 - Mock)
- **도메인**: `ShoppingProduct.java`
- **API**:
    - `GET /api/shopping/search`: 키워드 기반 상품 검색.
    - `GET /api/shopping/recommendations`: 식재료 기반 상품 추천.
- **현재 상태**: `ElevenstShoppingClient`는 실제 API를 호출하지 않는 **Mock 구현**입니다. 메모리에 하드코딩된 샘플 상품 목록을 반환하므로, 항상 비슷한 결과가 나옵니다.
- **향후 계획**: `ElevenstShoppingClient` 내부를 `RestTemplate` 또는 `WebClient`를 사용한 실제 11번가 API 호출 로직으로 교체할 예정입니다. 이 변경은 `ShoppingService` 등 상위 계층에 영향을 주지 않습니다.

### 5.3. 식단(MealPlan) 도메인 (구현 예정)
- **목표**: 사용자의 `targetCalories`에 맞춰 월간/주간 식단을 자동 생성.
- **엔티티(예정)**: `MealPlan`, `MealPlanDay`, `MealItem`
- **API(예정)**:
    - `POST /api/meal-plans`: 월간 식단 생성
    - `GET /api/meal-plans/{planId}`: 특정 식단 조회
    - `GET /api/users/{userId}/meal-plans/latest`: 사용자의 최신 식단 조회

### 5.4. 체중 기록(WeightRecord) 도메인 (구현 예정)
- **목표**: 사용자의 일일 체중 변화를 기록하고 추적.
- **엔티티(예정)**: `WeightRecord`
- **API(예정)**:
    - `POST /api/weights`: 오늘의 체중 기록
    - `GET /api/users/{userId}/weights/recent?days=14`: 최근 N일간의 체중 기록 조회

### 5.5. 3D 캐릭터 시각화 (프론트엔드 연동 예정)
- **목표**: 사용자의 체중 변화를 3D 캐릭터의 모습에 반영하여 시각적인 피드백 제공.
- **백엔드 연동 포인트(예정)**:
    - `GET /api/users/{userId}/weights/recent`
    - `GET /api/users/{userId}/tdee`

---

## 6. 핵심 소스 코드
(기존과 동일하게 `pom.xml`, `UserController.java`, `UserService.java`, `UserServiceImpl.java`, `UserMapper.java`, `UserMapper.xml` 코드 포함)
... (생략) ...
```

**수정 완료!**

`test.md`의 모든 요구사항을 반영하여 `PROJECT_BRIEF_DETAILED.md` 파일을 위 내용으로 갱신했습니다.

이제 이 문서는 프로젝트의 현재 상태, 규칙, 향후 계획을 매우 상세하고 명확하게 담고 있습니다. 이 문서를 기반으로 다음 작업을 지시하시면 AI가 훨씬 더 정확하게 의도를 파악하고 코드를 생성할 것입니다.

다음 작업을 알려주세요.
# Diet Coach 프로젝트 브리핑 (AI 개발자용)

## 1. 프로젝트 개요

**Diet Coach**는 사용자의 신체 정보와 활동 수준을 바탕으로 TDEE(총 일일 에너지 소비량)를 계산하고, 이를 기반으로 다이어트 목표 설정 및 식단 관련 상품 추천을 제공하는 Spring Boot 기반 웹 애플리케이션입니다.

- **핵심 목표**: 사용자 맞춤형 건강 관리 및 다이어트 보조
- **주요 기능**:
    - 사용자 정보(신체, 목표 등) 등록 및 조회
    - 기초대사량(BMR), 총 일일 에너지 소비량(TDEE), 목표 섭취 칼로리 계산
    - 외부 쇼핑몰 API와 연동하여 키워드 기반 상품 검색 및 추천

---

## 2. 기술 스택

`pom.xml`을 기반으로 파악된 주요 기술 스택은 다음과 같습니다.

- **언어**: Java 17
- **프레임워크**: Spring Boot 3.5.8
- **주요 의존성**:
    - `spring-boot-starter-web`: RESTful API 개발
    - `spring-boot-starter-validation`: 데이터 유효성 검증
    - `mybatis-spring-boot-starter`: MyBatis 연동 (ORM/Data Mapper)
    - `mysql-connector-j`: MySQL 데이터베이스 드라이버
    - `lombok`: 보일러플레이트 코드 감소
- **빌드 도구**: Maven

---

## 3. 프로젝트 구조

전형적인 계층형 아키텍처(Layered Architecture)를 따릅니다.

- `src/main/java/com/dietcoach/project/`
    - `controller`: API 엔드포인트를 정의하고 HTTP 요청/응답을 처리합니다.
    - `service`: 핵심 비즈니스 로직을 구현합니다.
    - `domain`: `User`와 같이 데이터베이스 테이블과 매핑되는 핵심 객체를 정의합니다.
    - `dto`: API 계층에서 데이터 전송을 위해 사용되는 객체(Request/Response)를 정의합니다.
    - `mapper`: MyBatis의 Mapper 인터페이스로, SQL 쿼리를 호출합니다.
    - `common`: `ApiResponse`, `TdeeCalculator` 등 프로젝트 전반에서 사용되는 공통 유틸리티 클래스를 포함합니다.
    - `client`: `ElevenstShoppingClient`와 같이 외부 API와 통신하는 클라이언트를 포함합니다.
- `src/main/resources/`
    - `application.properties`: 애플리케이션의 주요 설정을 담당합니다.
    - `schema/schema.sql`: 데이터베이스 테이블 구조(DDL)를 정의합니다.
    - `mapper/UserMapper.xml`: MyBatis에서 사용할 SQL 쿼리를 XML 형태로 작성합니다.

---

## 4. 핵심 기능 및 API 명세

### 4.1. Health Check API

- **Controller**: `HealthController.java`
- **Endpoint**: `GET /api/health`
- **설명**: 서버의 현재 동작 상태를 확인하는 헬스 체크 API입니다.
- **성공 응답**:
    ```json
    {
      "message": "dietcoach-backend is up 🚀",
      "data": "UP"
    }
    ```

### 4.2. User API

- **Controller**: `UserController.java`
- **Endpoint**: `POST /api/users`
    - **설명**: 신규 사용자를 생성합니다. 요청 시 사용자의 신체 정보, 활동량, 목표 등을 받아 TDEE를 계산하고 데이터베이스에 저장합니다.
    - **Request Body**: `UserCreateRequest` DTO
    - **성공 응답**: 생성된 사용자의 `id` (Long)
- **Endpoint**: `GET /api/users/{id}`
    - **설명**: 특정 사용자의 프로필 정보(TDEE 포함)를 조회합니다.
    - **성공 응답**: `UserProfileResponse` DTO
- **Endpoint**: `GET /api/users/{id}/tdee`
    - **설명**: 특정 사용자의 TDEE 관련 정보만 별도로 조회합니다.
    - **성공 응답**: `TdeeResponse` DTO

### 4.3. Shopping API

- **Controller**: `ShoppingController.java`
- **Endpoint**: `GET /api/shopping/search`
    - **설명**: 키워드를 기반으로 외부 쇼핑몰 상품을 검색합니다.
    - **Query Parameters**: `keyword` (필수), `page` (선택), `size` (선택)
    - **성공 응답**: `List<ShoppingProductResponse>`
- **Endpoint**: `GET /api/shopping/recommendations`
    - **설명**: 특정 식재료(ingredient)를 기반으로 상품을 추천합니다.
    - **Query Parameters**: `ingredient` (필수), `neededGram` (선택)
    - **성공 응답**: `List<ShoppingProductResponse>`

---

## 5. 데이터베이스 스키마

- **파일 위치**: `src/main/resources/schema/schema.sql`
- **테이블**: `users`

```sql
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    gender           VARCHAR(10)  NOT NULL,      -- MALE / FEMALE / OTHER
    birth_date       DATE         NOT NULL,
    height           DOUBLE       NOT NULL,
    weight           DOUBLE       NOT NULL,
    activity_level   VARCHAR(20)  NOT NULL,      -- SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE
    goal_type        VARCHAR(20)  NOT NULL,      -- LOSE_WEIGHT / MAINTAIN / GAIN_WEIGHT
    bmr              DOUBLE       NULL,          -- 계산된 BMR
    tdee             DOUBLE       NULL,          -- 계산된 TDEE
    target_calories  DOUBLE       NULL,          -- 목표 칼로리(TDEE±알파)
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 6. 개발 가이드 (for AI)

이 프로젝트에서 새로운 기능을 추가하거나 코드를 수정할 때 다음 가이드라인을 따라주세요.

1.  **컨벤션 준수**: 기존 코드의 스타일과 패턴(네이밍, 계층 구조 등)을 철저히 따릅니다.
2.  **계층형 아키텍처 유지**:
    - **Controller**: 클라이언트의 요청/응답 처리 및 데이터 유효성 검증만 담당합니다.
    - **Service**: 비즈니스 로직을 집중적으로 처리합니다. Controller나 Mapper의 역할을 침범하지 않도록 주의합니다.
    - **Mapper/Domain**: 데이터베이스와의 상호작용 및 데이터 표현에만 집중합니다.
3.  **DTO 사용**: Controller의 API 응답으로는 항상 DTO(`*Response`)를 사용하고, 요청 또한 DTO(`*Request`)를 사용합니다. `Domain` 객체를 외부에 직접 노출하지 않습니다.
4.  **공통 응답 형식**: 모든 API 응답은 `common/ApiResponse` 클래스로 감싸서 일관된 응답 형식을 유지합니다.
5.  **TDD (Test-Driven Development)**: 새로운 기능을 추가할 때는 `src/test` 경로에 해당 기능에 대한 테스트 코드를 먼저 작성하거나, 기능 구현 후 반드시 테스트 코드를 추가하여 안정성을 확보하는 것을 권장합니다.
6.  **명확한 커밋 메시지**: 변경 사항에 대한 커밋 메시지는 명확하고 간결하게 작성합니다. (예: `feat: Add user profile update feature`)

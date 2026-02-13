# 🏛 Project Architecture

이 문서는 **Diet Coach** 프로젝트의 기술적 설계와 구조에 대해 설명합니다.

## 1. 개요 (Overview)
본 프로젝트는 확장성과 유지보수성을 고려하여 **Layered Architecture** 패턴을 기반으로 설계되었습니다. 백엔드와 프론트엔드는 분리되어 있으며 RESTful API를 통해 통신합니다.

## 2. Backend Architecture (Spring Boot)
백엔드는 역할에 따라 4개의 주요 레이어로 구분됩니다.

### Layers
- **Controller (Web Layer)**: 
  - 외부 요청(HTTP Request)을 수신하고 응답(Response)을 반환합니다.
  - 요청 파라미터 유효성 검사(Validation)를 수행합니다.
- **Service (Business Layer)**: 
  - 핵심 비즈니스 로직(TDEE 계산, 식단 생성 알고리즘 등)을 구현합니다.
  - 트랜잭션 관리와 도메인 간의 로직 흐름을 제어합니다.
- **Mapper/Repository (Data Access Layer)**: 
  - MyBatis를 사용하여 DB와의 인터페이스를 정의합니다.
  - `resources/mapper`에 정의된 XML 쿼리와 매핑됩니다.
- **Domain/DTO (Data Object)**: 
  - **Domain**: DB 테이블과 1:1 대응되는 핵심 모델입니다.
  - **DTO**: API 요청/응답 시 필요한 데이터만 골라 담는 객체로, 보안과 데이터 효율성을 위해 Domain과 분리하여 사용합니다.

### Security
- **JWT Authentication**: Spring Security와 연동하여 무상태(Stateless) 인증을 구현했습니다.
- **RTR Policy**: 보안 강화를 위해 Refresh Token Rotation 정책을 적용했습니다.

## 3. Frontend Architecture (Vue.js)
프론트엔드는 컴포넌트 기반 아키텍처를 따르며, 관심사 분리를 위해 폴더 구조를 최적화했습니다.

### Components Structure
- **Pages**: 라우터와 연결된 최상위 뷰 컴포넌트입니다.
- **Components/Common**: 여러 페이지에서 재사용되는 버튼, 입력창, 모달 등의 UI 요소입니다.
- **API Layer**: `axios` 인스턴스를 관리하고 모든 백엔드 통신 로직을 모듈화하여 관리합니다.
- **Assets/Models**: Three.js에서 사용하는 3D 모델(.glb) 및 정적 자산을 보관합니다.

## 4. 데이터베이스 설계 전략
- **Normalization**: 데이터 중복 최소화를 위해 정규화를 수행했습니다.
- **Integrity**: 외래키(Foreign Key) 제약 조건을 통해 데이터 무결성을 보장합니다.
- **Scalability**: 추후 식단 종류 확장이나 사용자 통계 기능을 고려하여 테이블을 설계했습니다.

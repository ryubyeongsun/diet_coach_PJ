# 프로젝트 구조

이 문서는 'Diet Coach' 프로젝트의 전체적인 디렉터리 구조와 각 부분의 역할을 설명합니다.

## 1. 최상위 디렉터리 구조

```
diet_coach_PJ/
├── .git/               # Git 버전 관리 시스템 디렉터리
├── frontend/           # Vue.js 프론트엔드 애플리케이션 루트
├── src/                # Spring Boot 백엔드 애플리케이션 루트
├── .gitignore          # Git 커밋 시 제외할 파일 및 디렉터리 목록
├── mvnw, mvnw.cmd      # Maven Wrapper 스크립트
├── pom.xml             # Maven 프로젝트 설정 파일 (의존성, 빌드 정보)
└── ... (기타 프로젝트 문서)
```

## 2. 백엔드 (`src`)

Spring Boot 기반의 Java 애플리케이션으로, 비즈니스 로직과 API를 담당합니다. 전형적인 계층형 아키텍처를 따릅니다.

-   **`src/main/java/com/dietcoach/project`**: 실제 Java 소스 코드가 위치합니다.
    -   `controller`: API 엔드포인트를 정의하고 HTTP 요청을 처리합니다.
    -   `service`: 핵심 비즈니스 로직을 구현합니다.
    -   `domain`: 데이터베이스 테이블과 매핑되는 핵심 도메인 객체를 정의합니다.
    -   `dto`: 데이터 전송 객체(Data Transfer Object)로, API 계층에서 사용됩니다.
    -   `mapper`: MyBatis의 Mapper 인터페이스로, SQL 쿼리를 호출합니다.
    -   `config`: 애플리케이션의 보안, CORS 등 각종 설정을 구성합니다.
    -   `security`: Spring Security와 JWT 관련 로직을 포함합니다.
    -   `common`: 예외 처리, 공통 응답 형식 등 프로젝트 전반에서 사용되는 유틸리티를 포함합니다.
-   **`src/main/resources`**: 설정 파일, 정적 리소스, MyBatis 쿼리 파일이 위치합니다.
    -   `application.yml`: Spring Boot 애플리케이션의 주요 설정 파일입니다.
    -   `mapper/`: MyBatis의 SQL 쿼리가 담긴 XML 파일들이 위치합니다.
    -   `schema/`: 데이터베이스 스키마 정의(`schema.sql`) 파일이 위치합니다.
-   **`src/test/java`**: 단위 테스트 및 통합 테스트 코드가 위치합니다.

## 3. 프론트엔드 (`frontend`)

Vite 기반의 Vue.js 싱글 페이지 애플리케이션(SPA)으로, 사용자 인터페이스(UI)를 담당합니다.

-   **`frontend/src`**: 실제 Vue.js 소스 코드가 위치합니다.
    -   `api/`: 백엔드 API와의 통신을 담당하는 모듈(Axios 인스턴스)이 위치합니다.
    -   `assets/`: 이미지, 폰트 등 정적 에셋 파일이 위치합니다.
    -   `components/`: 여러 페이지에서 재사용 가능한 UI 컴포넌트(버튼, 카드, 차트 등)가 위치합니다.
    -   `pages/`: Vue Router에 의해 매핑되는 개별 페이지 컴포넌트가 위치합니다.
    -   `router/`: 클라이언트 사이드 라우팅 설정이 위치합니다.
    -   `utils/`: 인증 토큰 관리 등 전역적으로 사용되는 유틸리티 함수가 위치합니다.
    -   `App.vue`: 애플리케이션의 최상위 루트 컴포넌트입니다.
    -   `main.js`: Vue 인스턴스를 생성하고 애플리케이션을 초기화하는 진입점입니다.
-   **`frontend/public`**: 빌드 과정에서 그대로 복사되는 정적 파일(favicon 등)이 위치합니다.
-   **`package.json`**: 프론트엔드 프로젝트의 의존성 및 스크립트 정보를 정의합니다.
-   **`vite.config.js`**: Vite 빌드 도구의 설정 파일입니다.

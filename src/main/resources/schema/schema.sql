
use yumyum;

CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,      -- nickname → name으로 변경
    gender           VARCHAR(10)  NOT NULL,      -- MALE / FEMALE / OTHER
    birth_date       DATE         NOT NULL,      -- age 대신 birth_date 사용
    height           DOUBLE       NOT NULL,      -- height_cm → height
    weight           DOUBLE       NOT NULL,      -- weight_kg → weight
    activity_level   VARCHAR(20)  NOT NULL,      -- SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE
    goal_type        VARCHAR(20)  NOT NULL,      -- LOSE_WEIGHT / MAINTAIN / GAIN_WEIGHT
    bmr              DOUBLE       NULL,          -- 계산된 BMR
    tdee             DOUBLE       NULL,          -- 계산된 TDEE
    target_calories  DOUBLE       NULL,          -- 목표 칼로리(TDEE±알파)
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS meal_plans (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                  BIGINT      NOT NULL,       -- users.id FK
    start_date               DATE        NOT NULL,       -- 식단 시작일
    end_date                 DATE        NOT NULL,       -- 식단 종료일
    total_days               INT         NOT NULL,       -- 전체 일수
    target_calories_per_day  DOUBLE      NOT NULL,       -- 1일 목표 kcal
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS meal_plan_days (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_plan_id    BIGINT      NOT NULL,               -- meal_plans.id FK
    plan_date       DATE        NOT NULL,               -- 해당 날짜
    day_index       INT         NOT NULL,               -- 0,1,2,... 순번 (정렬용) ⭐ 추가 추천
    total_calories  DOUBLE      NULL,                   -- 그날 총 kcal (계산 결과)
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS meal_items (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_plan_day_id  BIGINT      NOT NULL,             -- meal_plan_days.id FK
    meal_time         VARCHAR(20) NOT NULL,             -- BREAKFAST / LUNCH / DINNER / SNACK
    food_name         VARCHAR(100) NOT NULL,            -- 음식 이름
    calories          DOUBLE      NOT NULL,             -- kcal
    memo              VARCHAR(255) NULL,                -- 메모(선택)
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);



SELECT id, email, name, gender, birth_date, height, weight,
       activity_level, goal_type, bmr, tdee, target_calories
FROM users;
select *from users;
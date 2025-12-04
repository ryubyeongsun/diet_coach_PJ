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

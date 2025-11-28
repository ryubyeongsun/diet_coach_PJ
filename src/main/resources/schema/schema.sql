CREATE DATABASE yumyum;

CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    nickname         VARCHAR(50)  NOT NULL,
    gender           VARCHAR(10)  NOT NULL,      -- MALE / FEMALE / OTHER
    age              INT          NOT NULL,
    height_cm        DOUBLE       NOT NULL,
    weight_kg        DOUBLE       NOT NULL,
    activity_level   VARCHAR(20)  NOT NULL,      -- SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE
    goal_type        VARCHAR(20)  NOT NULL,      -- LOSE / MAINTAIN / GAIN
    target_weight_kg DOUBLE       NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

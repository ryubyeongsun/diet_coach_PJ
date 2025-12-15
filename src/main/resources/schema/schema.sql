
use yumyum;

CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,      -- nickname → name으로 변경
    gender           VARCHAR(10)  NULL,      -- MALE / FEMALE / OTHER
    birth_date       DATE         NULL,      -- age 대신 birth_date 사용
    height           DOUBLE       NULL,      -- height_cm → height
    weight           DOUBLE       NULL,      -- weight_kg → weight
    activity_level   VARCHAR(20)  NULL,      -- SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE
    goal_type        VARCHAR(20)  NULL,      -- LOSE_WEIGHT / MAINTAIN / GAIN_WEIGHT
    bmr              DOUBLE       NULL,          -- 계산된 BMR
    tdee             DOUBLE       NULL,          -- 계산된 TDEE
    target_calories  DOUBLE       NULL,          -- 목표 칼로리(TDEE±알파)
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- 1) meal_plans
CREATE TABLE meal_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INT NOT NULL,
    target_calories_per_day INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_meal_plans_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 2) meal_plan_days
CREATE TABLE meal_plan_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_plan_id BIGINT NOT NULL,
    plan_date DATE NOT NULL,
    day_index INT NOT NULL,
    total_calories INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_meal_plan_days_meal_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plans(id)
);

-- 3) meal_items
CREATE TABLE meal_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_plan_day_id BIGINT NOT NULL,
    meal_time VARCHAR(20) NOT NULL,   -- BREAKFAST / LUNCH / DINNER / SNACK 등
    food_name VARCHAR(255) NOT NULL,
    calories INT NOT NULL,
    memo VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_meal_items_meal_plan_day
        FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_days(id)
);
ALTER TABLE meal_items
    ADD COLUMN grams INT NOT NULL DEFAULT 100;


SELECT id, email, name, gender, birth_date, height, weight,
       activity_level, goal_type, bmr, tdee, target_calories
FROM users;
CREATE TABLE IF NOT EXISTS weight_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  weight DECIMAL(5,2) NOT NULL,
  memo VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, record_date),
  CONSTRAINT fk_weight_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
select *from users;
SELECT * FROM users;
SELECT * FROM meal_plans;
SELECT * FROM meal_plan_days;
SELECT * FROM meal_items;

SELECT *
FROM meal_items
WHERE meal_plan_day_id = 1;
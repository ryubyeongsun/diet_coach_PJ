drop database yumyum;
create database yumyum;
USE yumyum;

-- Drop tables in reverse order of creation to avoid foreign key constraints
DROP TABLE IF EXISTS meal_intakes;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS shopping_cart_items;
DROP TABLE IF EXISTS meal_items;
DROP TABLE IF EXISTS meal_plan_days;
DROP TABLE IF EXISTS meal_plans;
DROP TABLE IF EXISTS weight_records;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    gender           VARCHAR(10)  NULL,
    birth_date       DATE         NULL,
    height           DOUBLE       NULL,
    weight           DOUBLE       NULL,
    activity_level   VARCHAR(20)  NULL,
    goal_type        VARCHAR(20)  NULL,
    bmr              DOUBLE       NULL,
    tdee             DOUBLE       NULL,
    target_calories  DOUBLE       NULL,
    target_weight    DOUBLE       NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create weight_records table
CREATE TABLE weight_records (
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

-- Create meal_plans table
CREATE TABLE meal_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INT NOT NULL,
    target_calories_per_day INT NOT NULL,
    monthly_budget BIGINT NULL,
    meals_per_day INT NULL,
    preferences TEXT NULL,
    allergies TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_meal_plans_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create meal_plan_days table
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

-- Create meal_items table
CREATE TABLE meal_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_plan_day_id BIGINT NOT NULL,
    meal_time VARCHAR(20) NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    calories INT NOT NULL,
    grams INT NOT NULL DEFAULT 100,
    memo VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_meal_items_meal_plan_day
        FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_days(id)
);

-- Create meal_intakes table
CREATE TABLE meal_intakes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  meal_plan_day_id BIGINT NOT NULL,
  meal_time VARCHAR(20) NOT NULL,
  is_consumed TINYINT(1) NOT NULL DEFAULT 0,
  consumed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_intake (user_id, meal_plan_day_id, meal_time),
  CONSTRAINT fk_intake_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_intake_day FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_days(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create shopping_cart_items table
CREATE TABLE shopping_cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_code VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    image_url VARCHAR(512),
    product_url VARCHAR(512),
    mall_name VARCHAR(100),
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_product (user_id, product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_refresh_token_hash (token_hash),
  INDEX idx_refresh_user_id (user_id),
  INDEX idx_refresh_expires_at (expires_at)
);

select *from users;
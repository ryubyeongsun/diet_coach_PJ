// src/utils/bmi.js

/**
 * Calculates BMI from weight and height.
 * @param {number} weight - Weight in kg.
 * @param {number} height - Height in cm.
 * @returns {number} BMI value, rounded to 1 decimal place.
 */
export function calculateBmi(weight, height) {
  if (!weight || !height) return 0;
  const heightInMeters = height / 100;
  return parseFloat((weight / (heightInMeters * heightInMeters)).toFixed(1));
}

/**
 * Determines BMI category based on BMI value.
 * @param {number} bmi - The BMI value.
 * @returns {string} BMI category name.
 */
export function getBmiCategory(bmi) {
  if (bmi < 18.5) return '저체중';
  if (bmi < 23) return '정상';
  if (bmi < 25) return '과체중';
  if (bmi < 30) return '비만';
  return '고도비만';
}

/**
 * Maps BMI category to a character level (1-5).
 * Matches PRD:
 * LV1: 매우 마름 (< 17.0)
 * LV2: 마름 (17.0 ~ 18.4)
 * LV3: 정상 (18.5 ~ 22.9)
 * LV4: 통통 (23.0 ~ 24.9)
 * LV5: 비만 (25.0 ~)
 * @param {number} bmi - The BMI value.
 * @returns {number} Character level (1-5).
 */
export function getCharacterLevel(bmi) {
  if (bmi < 17.0) return 1;
  if (bmi < 18.5) return 2;
  if (bmi < 23.0) return 3;
  if (bmi < 25.0) return 4;
  return 5;
}

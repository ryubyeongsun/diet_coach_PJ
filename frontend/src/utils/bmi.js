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
 * Maps BMI category to a character level.
 * @param {string} category - The BMI category name.
 * @returns {number} Character level (1-5).
 */
export function getCharacterLevel(category) {
  switch (category) {
    case '저체중': return 1;
    case '정상': return 3;
    case '과체중': return 2;
    case '비만': return 4;
    case '고도비만': return 5;
    default: return 3;
  }
}

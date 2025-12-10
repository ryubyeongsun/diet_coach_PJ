# API 응답 및 프론트엔드 네이밍 컨벤션

## 1. 문서의 목적

백엔드 API와 프론트엔드 애플리케이션 간의 데이터 교환 시, 필드(속성)명의 불일치로 인해 발생하는 버그를 최소화하고, 개발 생산성을 높이며, 코드의 일관성을 유지하는 것을 목적으로 한다.

---

## 2. 일반 규칙

1.  **언어**: 모든 필드명은 **영어**로 작성한다.
2.  **표기법(Case)**: JSON 속성, JavaScript/TypeScript 변수 및 객체 속성은 모두 **카멜 케이스(camelCase)**를 사용한다.
    -   예시: `mealPlanId` (O), `meal_plan_id` (X), `MealPlanId` (X)

---

## 3. 필드명 상세 규칙

### 가. ID (식별자) 필드

-   **기본 ID**: 해당 객체의 고유 식별자는 `id`로 명명한다.
    -   예시: `User` 객체의 ID는 `{ "id": 1, ... }`
-   **참조 ID (외래 키)**: 다른 객체를 참조하는 ID는 `{참조하는 객체명}Id` 형식으로 명명한다.
    -   예시: `MealPlan` 객체 내의 사용자 참조 ID는 `{ "userId": 1, ... }`
-   **엔티티 ID 명확화**: 여러 ID가 혼재될 수 있는 경우, 혼동을 막기 위해 엔티티 이름을 접두사로 사용한다.
    -   예시: `MealPlan`의 고유 ID는 `id` 대신 `planId`로 명명하여 `dayId`, `itemId`와의 혼동을 방지한다.
        - **(적용 사례)**: 이전에 `planId`와 `mealPlanId`가 혼용되어 문제가 발생했으므로, 앞으로 식단 플랜의 ID는 **`planId`**로 통일한다.

### 나. 이름 및 명칭 필드

-   객체의 이름이나 명칭을 나타낼 때는 `{객체명}Name` 형식을 권장한다.
-   예시: `ingredientName`, `productName`, `userName`
    -   **(적용 사례)**: 재료 이름은 `ingredient`가 아닌 **`ingredientName`**으로 통일한다.

### 다. 수량 및 개수 필드

-   개수나 수량을 나타내는 필드는 접미사로 `Count`를 붙인다.
-   예시: `daysCount`, `itemsCount`

### 라. 합계 및 총량 필드

-   합계나 총량을 나타내는 필드는 접두사로 `total`을 붙인다.
-   예시: `totalCalories`, `totalGram`
    -   **(적용 사례)**: 필요한 그램 총량은 `neededGram`이 아닌 **`totalGram`**으로 통일한다.

### 마. 날짜 및 시간 필드

-   날짜를 나타내는 필드는 접미사로 `Date`를 붙인다.
-   데이터 형식은 **ISO 8601 문자열** (`YYYY-MM-DD` 또는 `YYYY-MM-DDTHH:mm:ss.sssZ`)을 사용한다.
-   예시: `startDate`, `endDate`, `birthDate`

---

## 4. DTO (Data Transfer Object) 예시

아래 예시는 위 규칙을 적용한 이상적인 DTO 구조입니다. 향후 모든 API 응답은 이 구조를 따르는 것을 목표로 합니다.

#### `MealPlanOverviewResponse`
```json
{
  "planId": 1,
  "userId": 1,
  "startDate": "2025-12-10",
  "endDate": "2026-01-08",
  "totalDays": 30,
  "targetCaloriesPerDay": 2056,
  "days": [
    {
      "dayId": 101,
      "date": "2025-12-10",
      "totalCalories": 2056
    }
  ]
}
```

#### `MealPlanDayDetailResponse`
```json
{
  "dayId": 101,
  "date": "2025-12-10",
  "totalCalories": 2056,
  "items": [
    {
      "id": 1001,
      "mealTime": "BREAKFAST",
      "foodName": "오트밀 요거트",
      "calories": 450,
      "memo": "견과류 포함"
    }
  ]
}
```

#### `MealPlanIngredientResponse`
```json
{
  "ingredientName": "닭가슴살",
  "totalGram": 9000,
  "totalCalories": 18690,
  "daysCount": 30
}
```

# AvoFit API 목록

실제 Controller 기준 경로와 입력 요약입니다. 날짜 형식은 `YYYY-MM-DD`입니다. 정상 반환은 현재 Controller 기준 HTTP 200이며 일반 응답은 다음 형태입니다.

```json
{"success": true, "message": "success", "data": {}}
```

`data` 타입과 필수 입력 검증은 각 DTO·Service를 함께 확인해야 합니다. 사용자·체중·식단 리소스는 서비스 계층에서 인증 사용자의 소유 여부를 검사하고 타 사용자 접근은 403을 반환합니다. 남은 보완 사항은 [아키텍처](ARCHITECTURE.md)를 참고하세요.

## 인증

보호 API는 `Authorization: Bearer <accessToken>`을 사용합니다. 공개 경로는 `/api/health/**`, `/api/auth/signup`, `/api/auth/login`, `/api/auth/refresh`입니다. `GET /api/auth/me`를 포함한 보호 API에는 Access Token이 필요합니다. Refresh Token을 일반 API의 Bearer 토큰으로 사용할 수 없습니다. 파싱·기존 DTO 검증 오류는 400을 반환합니다.

| Method | Path | 입력 | data |
| --- | --- | --- | --- |
| GET | `/api/health` | 없음 | `UP` |
| POST | `/api/auth/signup` | `SignupRequest`: email, password, name | 사용자 ID |
| POST | `/api/auth/login` | `LoginRequest`: email, password | `AuthResponse` |
| POST | `/api/auth/refresh` | `RefreshRequest`: refreshToken | `AuthResponse` |
| GET | `/api/auth/me` | Access Token | `UserProfileResponse` |

## 사용자·체중

| Method | Path | 입력 | data |
| --- | --- | --- | --- |
| POST | `/api/users` | `UserCreateRequest` | 사용자 ID |
| GET | `/api/users/{id}` | 사용자 ID | `UserProfileResponse` |
| PUT | `/api/users/{id}/profile` | `UserProfileUpdateRequest` | `UserProfileResponse` |
| GET | `/api/users/{id}/tdee` | 사용자 ID | `TdeeResponse` |
| GET | `/api/users/{id}/body-status` | 사용자 ID | `BodyStatusResponse` |
| GET | `/api/users/{userId}/weights` | 선택 query: `from`, `to` | `List<WeightRecordResponse>` |
| POST | `/api/users/{userId}/weights` | `recordDate`, `weight`, `memo` | `WeightRecordResponse` |
| DELETE | `/api/users/{userId}/weights/{recordDate}` | 날짜 | null |

체중 POST는 사용자·날짜 기준 upsert입니다. 기존 문서의 개별 날짜 GET과 recordId 기반 PUT은 현재 Controller에 없습니다.

```json
{"recordDate": "2026-10-28", "weight": 70.0, "memo": "아침 기록"}
```

## 식단·섭취·대시보드

| Method | Path | 입력 | data |
| --- | --- | --- | --- |
| POST | `/api/meal-plans` | `MealPlanCreateRequest` | `MealPlanOverviewResponse` |
| GET | `/api/meal-plans/latest` | 인증 사용자 | `MealPlanOverviewResponse` |
| GET | `/api/users/{userId}/meal-plans/latest` | 사용자 ID | `MealPlanOverviewResponse` |
| GET | `/api/meal-plans/{planId}` | 플랜 ID | `MealPlanOverviewResponse` |
| GET | `/api/meal-plans/{planId}/ingredients` | 플랜 ID | `List<MealPlanIngredientResponse>` |
| GET | `/api/meal-plans/days/{dayId}` | 일자 ID | `MealPlanDayDetailResponse` |
| POST | `/api/meal-plans/days/{dayId}/regenerate` | 일자 ID | `MealPlanDayDetailResponse` |
| POST | `/api/meal-plans/days/{dayId}/meals/{mealTime}/replace` | 일자·끼니 | `MealPlanDayDetailResponse` |
| POST | `/api/meal-plans/days/{dayId}/stamp` | 일자 ID | null |
| PUT | `/api/meal-intakes` | `MealIntakeRequest` | `MealIntakeResponse` |
| GET | `/api/dashboard/summary` | 인증 사용자 | `DashboardSummaryResponse` |
| GET | `/api/dashboard/trend` | 필수 `userId`, 선택 `from`, `to` | `DashboardTrendResponse` |

식단 생성 body는 `startDate`, `monthlyBudget`, `mealsPerDay`, `preferences`, `allergies`를 포함할 수 있습니다. 사용자 ID는 인증 정보에서 가져옵니다.

```json
{
  "startDate": "2026-10-28",
  "monthlyBudget": 300000,
  "mealsPerDay": 3,
  "preferences": [],
  "allergies": []
}
```

첫 7일을 요청 중 생성하고 나머지 8~30일은 첫 저장 트랜잭션의 커밋 후 전용 Executor에서 순차 생성합니다. 백그라운드에서는 일자별 항목과 칼로리 합계를 하나의 트랜잭션으로 저장합니다. jobId·상태 API는 없습니다. 전체 조회에는 미생성 날짜가 포함될 수 있으며, 식단 조회를 다시 호출해 결과를 확인합니다.

## 장보기

| Method | Path | 입력 | data |
| --- | --- | --- | --- |
| GET | `/api/shopping/search` | 필수 `keyword`, 기본 `page=1`, `size=10` | `ShoppingProductsResponse` (`source`, `products`) |
| GET | `/api/meal-plans/{planId}/shopping` | 기본 `range=MONTH`, 선택 `date` | `ShoppingListResponse` |
| GET | `/api/meal-plans/{planId}/shopping-list` | 기본 `range=MONTH` | 위 경로의 호환 별칭 |

장보기 기간은 `TODAY`, `WEEK`, `MONTH`를 사용합니다. 추천 상품의 용량과 권장 개수는 추정치이며 전체 예산 초과 시 항목이 제외될 수 있습니다. 기존 문서의 `/api/shopping/products`는 현재 검색 경로가 아닙니다.

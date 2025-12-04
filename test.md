12.03 하루 작업 기준 역할 분담
💻 Dev A (주인님) – MealPlan 뼈대 잡기

오늘 목표: “MealPlan 도메인 스켈레톤 완성”

DB 설계 초안 정리 (sql or 노션)

meal_plans, meal_plan_days, meal_items 테이블 컬럼 확정만 해두기

아직 실제 DB 적용까지 못 해도 괜찮고, 스키마만 정리해두면 OK

Domain & DTO 클래스 생성

domain.MealPlan, MealPlanDay, MealItem 클래스 뼈대

dto.meal.MealPlanGenerateRequest

dto.meal.MealPlanSummaryResponse, MealPlanDetailResponse

Mapper & Service 인터페이스까지만

MealPlanMapper 인터페이스 메서드 시그니처 정의

insertMealPlan, insertMealPlanDay, insertMealItem

selectMealPlanById, selectLatestMealPlanByUserId

MealPlanService 인터페이스만 만들기

createMealPlan(MealPlanGenerateRequest request)

getMealPlan(Long mealPlanId)

getLatestMealPlan(Long userId)

Controller 엔드포인트만 먼저 만들기

MealPlanController 클래스 생성

아래 3개 메서드의 시그니처 & URL만 잡아두기 (아직 로직은 TODO)

POST /api/meal-plans

GET /api/meal-plans/{mealPlanId}

GET /api/users/{userId}/meal-plans/latest

👉 12.03 기준: **“식단 도메인 골격 + URL/DTO + 메서드 이름”**까지 맞춰놓는 게 목표.

💻 Dev B (페어) – WeightRecord 뼈대 + 3D용 API 포맷

오늘 목표: “WeightRecord 도메인 스켈레톤 + 3D용 데이터 API 틀”

DB 설계 초안 정리

weight_records 테이블 컬럼 확정

user_id, record_date, weight, bmi, created_at

Domain & DTO 클래스 생성

domain.WeightRecord

dto.weight.WeightRecordCreateRequest

dto.weight.WeightRecordResponse

Mapper & Service 인터페이스

WeightRecordMapper

insertWeightRecord, selectRecentWeightRecordsByUserId

WeightRecordService

createWeightRecord(WeightRecordCreateRequest request)

getRecentWeightRecords(Long userId, int days)

Controller 시그니처만

WeightRecordController

POST /api/weights

GET /api/users/{userId}/weights/recent?days=30

응답 포맷을 3D에서 쓰기 좋은 형태로 대략 맞춰만 두기
(예: date, weight, bmi 리스트)

👉 12.03 기준: **“체중 기록 도메인 구조 + 3D에서 쓸 데이터 포맷”**까지 잡는 게 목표.
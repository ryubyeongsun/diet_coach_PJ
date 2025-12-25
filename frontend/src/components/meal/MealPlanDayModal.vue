<template>
  <div v-if="modelValue" class="overlay" @click.self="closeModal">
    <div class="modal-card">
      <header class="modal-card__header">
        <div v-if="detail" class="header-row">
          <div>
            <p class="modal-card__date">{{ formatDate(detail.date) }}</p>
            <h2 class="modal-card__title">
              총 {{ detail.totalCalories }} kcal
            </h2>
          </div>
          <button
            class="regen-btn"
            @click="handleRegenerateDay"
            :disabled="isRegenerating"
            title="현재 식단이 마음에 들지 않으면 새로 추천받으세요"
          >
            <span class="regen-icon">🔄</span>
            <span class="regen-text">{{ isRegenerating ? "열심히 짜는 중... 🧑‍🍳" : "전체 다시 짜기" }}</span>
          </button>
        </div>
        <h2 v-else class="modal-card__title">하루 상세 식단</h2>
      </header>

      <!-- 탄단지 요약 차트 (추정치) -->
      <div v-if="detail && macroStats" class="macro-chart-section">
        <div class="macro-info">
          <div class="macro-item">
            <span class="macro-dot carb"></span>
            <span class="macro-label">탄수화물 {{ macroStats.carb }}%</span>
          </div>
          <div class="macro-item">
            <span class="macro-dot protein"></span>
            <span class="macro-label">단백질 {{ macroStats.protein }}%</span>
          </div>
          <div class="macro-item">
            <span class="macro-dot fat"></span>
            <span class="macro-label">지방 {{ macroStats.fat }}%</span>
          </div>
        </div>
        <div class="macro-bar">
          <div class="macro-bar-fill carb" :style="{ width: macroStats.carb + '%' }"></div>
          <div class="macro-bar-fill protein" :style="{ width: macroStats.protein + '%' }"></div>
          <div class="macro-bar-fill fat" :style="{ width: macroStats.fat + '%' }"></div>
        </div>
        <p class="macro-note">* 식재료명을 기반으로 추정한 대략적인 비율입니다.</p>
      </div>

      <div class="modal-card__body">
        <div v-if="isLoading" class="status-text">
          남남코치가 식단 정보를 가져오는 중이에요... 🥑
        </div>
        <div v-else-if="errorMessage" class="status-text error">
          {{ errorMessage }}
        </div>
        <div v-else-if="detail && groupedItems.length" class="item-groups">
          <div
            v-for="group in groupedItems"
            :key="group.key"
            class="item-group"
          >
            <div class="group-header">
              <h3 class="item-group__title">{{ group.title }}</h3>
              <button
                class="icon-btn"
                @click="handleReplaceMeal(group.key)"
                :disabled="replacingMeal === group.key"
                title="이 끼니만 다른 메뉴로 변경"
              >
                {{ replacingMeal === group.key ? "..." : "🔄 메뉴 변경" }}
              </button>
            </div>
            <ul class="item-list">
              <li v-for="item in group.items" :key="item.id" class="item">
                <div class="item__info">
                  <div class="item__name-row">
                    <span class="item__name">{{ item.foodName }}</span>
                  </div>
                  <span v-if="item.memo" class="item__memo">{{ item.memo }}</span>
                </div>
                <span class="item__kcal">
                  <span v-if="item.grams != null" class="item__gram">{{ item.grams }}g</span>
                  <span class="item__cal-val">{{ item.calories }} kcal</span>
                </span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <footer class="modal-card__footer">
        <NnButton 
          v-if="detail" 
          :variant="detail.isStamped ? 'secondary' : 'primary'" 
          block 
          @click="handleStamp"
          style="margin-bottom: 8px;"
        >
          {{ detail.isStamped ? "식단 완료 취소하기 ↩️" : "오늘 식단 완료! 💯" }}
        </NnButton>
        <NnButton block variant="outline" @click="closeModal">닫기</NnButton>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from "vue";
import { fetchDayDetail, regenerateDay, replaceMeal } from "../../api/mealPlanApi";
import NnButton from "../common/NnButton.vue";

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  dayId: {
    type: Number,
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "stamp"]);

const isLoading = ref(false);
const isRegenerating = ref(false);
const replacingMeal = ref(null);
const errorMessage = ref("");
const detail = ref(null);

const MEAL_TIME_MAP = {
  BREAKFAST: "아침",
  LUNCH: "점심",
  DINNER: "저녁",
  SNACK: "간식",
};

// 매크로 비율 계산 (백엔드 데이터 기반)
const macroStats = computed(() => {
  if (!detail.value || !detail.value.items) return null;
  
  let c = 0, p = 0, f = 0;
  
  detail.value.items.forEach(item => {
    // 백엔드에서 carbs, protein, fat이 0일 수 있으므로 안전하게 처리
    // 칼로리 기준 비율 (g * 4 or 9)
    const carbKcal = (item.carbs || 0) * 4;
    const proteinKcal = (item.protein || 0) * 4;
    const fatKcal = (item.fat || 0) * 9;

    c += carbKcal;
    p += proteinKcal;
    f += fatKcal;
  });

  const total = c + p + f;
  if (total === 0) return { carb: 0, protein: 0, fat: 0 };

  return {
    carb: Math.round((c / total) * 100),
    protein: Math.round((p / total) * 100),
    fat: Math.round((f / total) * 100)
  };
});

const groupedItems = computed(() => {
  if (!detail.value || !detail.value.items) return [];
  const order = ["BREAKFAST", "LUNCH", "DINNER", "SNACK"];
  return order
    .map((key) => {
      const items = detail.value.items.filter((i) => i.mealTime === key);
      if (items.length === 0) return null;
      return { key, title: MEAL_TIME_MAP[key], items };
    })
    .filter((g) => g !== null);
});

async function handleRegenerateDay() {
  if (!props.dayId || isRegenerating.value) return;
  if (!confirm("하루 식단이 모두 변경됩니다. 계속하시겠습니까?")) return;

  isRegenerating.value = true;
  try {
    const newDetail = await regenerateDay(props.dayId);
    detail.value = newDetail;
  } catch (err) {
    alert("식단 재생성에 실패했습니다.");
    console.error(err);
  } finally {
    isRegenerating.value = false;
  }
}

async function handleReplaceMeal(mealTime) {
  if (!props.dayId || replacingMeal.value) return;
  if (!confirm(`${MEAL_TIME_MAP[mealTime]} 메뉴만 변경하시겠습니까?`)) return;

  replacingMeal.value = mealTime;
  try {
    const newDetail = await replaceMeal(props.dayId, mealTime);
    detail.value = newDetail;
  } catch (err) {
    alert("끼니 교체에 실패했습니다.");
    console.error(err);
  } finally {
    replacingMeal.value = null;
  }
}

function handleStamp() {
  if (detail.value) {
    // 1. 로컬 상태 즉시 반전 (피드백)
    detail.value.isStamped = !detail.value.isStamped;
    
    // 2. 부모에게 이벤트 전달 (DB 저장 및 통계 갱신 요청)
    emit("stamp", props.dayId);
  }
}

watch(
  () => props.dayId,
  async (newDayId) => {
    if (newDayId && props.modelValue) {
      isLoading.value = true;
      errorMessage.value = "";
      detail.value = null;
      try {
        detail.value = await fetchDayDetail(newDayId);
      } catch (err) {
        console.error(err);
        errorMessage.value =
          "상세 식단 정보를 불러오는 중 오류가 발생했습니다.";
      } finally {
        isLoading.value = false;
      }
    }
  },
  { immediate: true },
);

function closeModal() {
  emit("update:modelValue", false);
}

function formatDate(isoString) {
  if (!isoString) return "";
  const d = new Date(isoString);
  const weekday = ["일", "월", "화", "수", "목", "금", "토"];
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 (${weekday[d.getDay()]})`;
}
</script>

<style scoped>
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  width: 100%;
  max-width: 500px;
  background-color: #ffffff;
  border-radius: 20px;
  box-shadow:
    0 25px 50px -12px rgb(0 0 0 / 0.25);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  overflow: hidden;
}

.modal-card__header {
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
  background: #fff;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

/* Regenerate Button */
.regen-btn {
  background-color: #f0fdf4; /* Green-50 */
  color: #15803d; /* Green-700 */
  border: 1px solid #bbf7d0; /* Green-200 */
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.regen-btn:hover:not(:disabled) {
  background-color: #dcfce7;
  transform: translateY(-1px);
}
.regen-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.regen-icon {
  font-size: 14px;
}

/* Macro Chart */
.macro-chart-section {
  padding: 16px 24px;
  background-color: #fafafa;
  border-bottom: 1px solid #f3f4f6;
}
.macro-info {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}
.macro-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.macro-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.macro-dot.carb { background-color: #fb923c; } /* Orange */
.macro-dot.protein { background-color: #3b82f6; } /* Blue */
.macro-dot.fat { background-color: #fbbf24; } /* Amber */

.macro-label {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
}

.macro-bar {
  display: flex;
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
  background-color: #e5e7eb;
}
.macro-bar-fill {
  height: 100%;
}
.macro-bar-fill.carb { background-color: #fb923c; }
.macro-bar-fill.protein { background-color: #3b82f6; }
.macro-bar-fill.fat { background-color: #fbbf24; }

.macro-note {
  margin: 6px 0 0;
  font-size: 11px;
  color: #9ca3af;
  text-align: right;
}

.modal-card__date {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 2px;
}

.modal-card__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #111827;
}

.modal-card__body {
  padding: 0 24px 20px;
  overflow-y: auto;
  flex-grow: 1;
}

.status-text {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  padding: 32px 0;
}
.status-text.error {
  color: #dc2626;
}

.item-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-top: 20px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}

/* Icon Button (Replace Meal) */
.icon-btn {
  background: #f3f4f6;
  border: none;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  color: #4b5563;
  transition: all 0.2s;
}
.icon-btn:hover:not(:disabled) {
  background-color: #e5e7eb;
  color: #111827;
}
.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.item-group__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.item__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item__name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.item__name {
  font-weight: 500;
  color: #111827;
  font-size: 15px;
}

.item__memo {
  font-size: 12px;
  color: #6b7280;
}

.item__kcal {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.item__gram {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 400;
}
.item__cal-val {
  color: #111827;
}

.modal-card__footer {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  background-color: #fff;
}
</style>

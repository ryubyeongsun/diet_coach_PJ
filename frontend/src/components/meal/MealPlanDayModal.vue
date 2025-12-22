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
          >
            {{ isRegenerating ? "생성 중.." : "🔄 하루 전체 다시 추천" }}
          </button>
        </div>
        <h2 v-else class="modal-card__title">하루 상세 식단</h2>
      </header>

      <div class="modal-card__body">
        <div v-if="isLoading" class="status-text">
          상세 식단 정보를 불러오는 중입니다...
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
                title="이 끼니만 바꾸기"
              >
                {{ replacingMeal === group.key ? "..." : "🔄" }}
              </button>
            </div>
            <ul class="item-list">
              <li v-for="item in group.items" :key="item.id" class="item">
                <div class="item__info">
                  <span class="item__name">{{ item.foodName }}</span>
                  <span v-if="item.memo" class="item__memo">{{
                    item.memo
                  }}</span>
                </div>
                <span class="item__kcal">
                  <span v-if="item.grams != null">{{ item.grams }}g · </span>
                  {{ item.calories }} kcal
                </span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <footer class="modal-card__footer">
        <NnButton block @click="closeModal">닫기</NnButton>
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

const emit = defineEmits(["update:modelValue"]);

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
  max-width: 480px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 20px 25px -5px rgb(0 0 0 / 0.1),
    0 8px 10px -6px rgb(0 0 0 / 0.1);
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.modal-card__header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.regen-btn {
  background-color: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.regen-btn:hover:not(:disabled) {
  background-color: #dbeafe;
}
.regen-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-card__date {
  font-size: 13px;
  color: #4b5563;
  margin: 0 0 2px;
}

.modal-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.modal-card__body {
  padding: 20px 24px;
  overflow-y: auto;
  flex-grow: 1;
}

.status-text {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  padding: 24px 0;
}
.status-text.error {
  color: #dc2626;
}

.item-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 6px;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  color: #6b7280;
}
.icon-btn:hover:not(:disabled) {
  background-color: #f3f4f6;
  color: #3b82f6;
}
.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.item-group__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
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

.item__name {
  font-weight: 500;
  color: #1f2937;
}

.item__memo {
  font-size: 12px;
  color: #6b7280;
}

.item__kcal {
  font-size: 14px;
  font-weight: 600;
  color: #166534;
  white-space: nowrap;
}

.modal-card__footer {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  background-color: #f9fafb;
}
</style>

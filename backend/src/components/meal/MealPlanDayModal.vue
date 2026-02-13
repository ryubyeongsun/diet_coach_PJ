<template>
  <div v-if="modelValue" class="overlay" @click.self="closeModal">
    <div class="modal-card">
      <header class="modal-card__header">
        <div v-if="detail">
          <p class="modal-card__date">{{ formatDate(detail.date) }}</p>
          <h2 class="modal-card__title">
            하루 상세 식단 (총 {{ detail.totalCalories }} kcal)
          </h2>
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
        <div v-else-if="detail && groupedItems" class="item-groups">
          <div
            v-for="(items, group) in groupedItems"
            :key="group"
            class="item-group"
          >
            <h3 class="item-group__title">{{ group }}</h3>
            <ul class="item-list">
              <li v-for="item in items" :key="item.id" class="item">
                <div class="item__info">
                  <span class="item__name">{{ item.foodName }}</span>
                  <span v-if="item.memo" class="item__memo">{{ item.memo }}</span>
                </div>
                <span class="item__kcal">{{ item.calories }} kcal</span>
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
import { ref, watch, computed } from 'vue';
import { fetchDayDetail } from '../../api/mealPlanApi';
import NnButton from '../common/NnButton.vue';

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

const emit = defineEmits(['update:modelValue']);

const isLoading = ref(false);
const errorMessage = ref('');
const detail = ref(null);

const MEAL_TIME_MAP = {
  BREAKFAST: '아침',
  LUNCH: '점심',
  DINNER: '저녁',
  SNACK: '간식',
};

const groupedItems = computed(() => {
  if (!detail.value || !detail.value.items) return null;
  return detail.value.items.reduce((acc, item) => {
    const groupName = MEAL_TIME_MAP[item.mealTime] || '기타';
    if (!acc[groupName]) {
      acc[groupName] = [];
    }
    acc[groupName].push(item);
    return acc;
  }, {});
});

watch(
  () => props.dayId,
  async (newDayId) => {
    if (newDayId && props.modelValue) {
      isLoading.value = true;
      errorMessage.value = '';
      detail.value = null;
      try {
        detail.value = await fetchDayDetail(newDayId);
      } catch (err) {
        console.error(err);
        errorMessage.value =
          '상세 식단 정보를 불러오는 중 오류가 발생했습니다.';
      } finally {
        isLoading.value = false;
      }
    }
  },
  { immediate: true }
);

function closeModal() {
  emit('update:modelValue', false);
}

function formatDate(isoString) {
  if (!isoString) return '';
  const d = new Date(isoString);
  const weekday = ['일', '월', '화', '수', '목', '금', '토'];
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
  box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1),
    0 8px 10px -6px rgb(0 0 0 / 0.1);
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.modal-card__header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
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

.item-group__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 6px;
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

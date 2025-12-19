<template>
  <div v-if="!isProfileCompleted" class="panel-disabled">
    <p>
      식단 생성을 위해
      <router-link to="/profile-setup">프로필을 먼저 완성</router-link>해주세요.
    </p>
  </div>
  <NnCard v-else title="30일 식단 생성" class="creation-panel">
    <form @submit.prevent="handleSubmit" class="form-grid">
      <div class="form-group">
        <label for="monthlyBudget">월 예산 (원)</label>
        <NnInput
          id="monthlyBudget"
          v-model.number="form.monthlyBudget"
          type="number"
          placeholder="예: 400000"
          required
          :min="1"
        />
      </div>

      <div class="form-group">
        <label for="mealsPerDay">일일 끼니 수</label>
        <select id="mealsPerDay" v-model.number="form.mealsPerDay" class="select-input">
          <option value="1">1끼</option>
          <option value="2">2끼</option>
          <option value="3">3끼</option>
        </select>
      </div>

      <div class="form-group">
        <label for="preferences">선호 음식</label>
        <NnInput
          id="preferences"
          v-model="form.preferences"
          type="text"
          placeholder="쉼표로 구분 (예: 닭가슴살, 밥)"
        />
      </div>

      <div class="form-group">
        <label for="allergies">알레르기 정보</label>
        <NnInput
          id="allergies"
          v-model="form.allergies"
          type="text"
          placeholder="쉼표로 구분 (예: 땅콩, 갑각류)"
        />
      </div>

      <div class="form-actions">
        <NnButton type="submit" block :disabled="isLoading">
          {{ buttonText }}
        </NnButton>
      </div>
    </form>
  </NnCard>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import NnInput from '../common/NnInput.vue';
import NnButton from '../common/NnButton.vue';
import NnCard from '../common/NnCard.vue';

const props = defineProps({
  user: {
    type: Object,
    default: null,
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  hasPlan: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['create-plan']);

const isProfileCompleted = ref(false);

const form = ref({
  monthlyBudget: null,
  mealsPerDay: 3,
  preferences: '',
  allergies: '',
});

const buttonText = computed(() => {
  if (props.isLoading) return '처리 중...';
  return props.hasPlan ? '식단 다시 생성하기' : '식단 생성하기';
});

watch(
  () => props.user,
  (newUser) => {
    isProfileCompleted.value = newUser?.isProfileCompleted ?? true;
  },
  { immediate: true }
);

function formatStringToArray(str) {
  if (!str) return [];
  return str.split(',').map(item => item.trim()).filter(item => item);
}

function handleSubmit() {
  if (props.isLoading) return;

  if (props.hasPlan) {
    if (!confirm('기존 식단이 삭제되고 새로 생성됩니다. 계속하시겠습니까?')) {
      return;
    }
  }

  const payload = {
    monthlyBudget: form.value.monthlyBudget,
    mealsPerDay: form.value.mealsPerDay,
    preferences: formatStringToArray(form.value.preferences),
    allergies: formatStringToArray(form.value.allergies),
  };

  emit('create-plan', payload);
}
</script>

<style scoped>
.panel-disabled {
  padding: 20px;
  text-align: center;
  background-color: #f3f4f6;
  border-radius: 8px;
  margin-bottom: 20px;
}
.panel-disabled p {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
}
.panel-disabled a {
  color: #1d4ed8;
  font-weight: 600;
  text-decoration: none;
}
.panel-disabled a:hover {
  text-decoration: underline;
}

.creation-panel {
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.select-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: #fff;
  font-size: 14px;
  height: 42px; /* NnInput과 높이 맞춤 */
}

.form-actions {
  grid-column: span 2;
}
</style>
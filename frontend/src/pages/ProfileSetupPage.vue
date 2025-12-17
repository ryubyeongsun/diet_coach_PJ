<template>
  <div class="profile-setup-page">
    <NnCard>
      <div class="header">
        <h1>프로필 정보 입력</h1>
        <p>정확한 식단 생성을 위해 아래 정보를 입력해주세요.</p>
      </div>

      <form @submit.prevent="handleSubmit" class="form">
        <div class="form-group">
          <label>성별</label>
          <div class="radio-group">
            <label><input type="radio" v-model="form.gender" value="MALE"> 남성</label>
            <label><input type="radio" v-model="form.gender" value="FEMALE"> 여성</label>
          </div>
        </div>

        <div class="form-group">
          <label for="birthDate">생년월일</label>
          <NnInput type="date" id="birthDate" v-model="form.birthDate" required />
        </div>

        <div class="form-group">
          <label for="height">키 (cm)</label>
          <NnInput type="number" id="height" v-model.number="form.height" placeholder="예: 175" required />
        </div>

        <div class="form-group">
          <label for="weight">현재 체중 (kg)</label>
          <NnInput type="number" id="weight" v-model.number="form.weight" placeholder="예: 70" required />
        </div>

        <div class="form-group">
          <label>활동량</label>
          <select v-model="form.activityLevel" class="select-input" required>
            <option disabled value="">활동량을 선택하세요</option>
            <option value="SEDENTARY">거의 운동 안 함</option>
            <option value="LIGHTLY_ACTIVE">가벼운 활동 (주 1-3일)</option>
            <option value="MODERATE">보통 활동 (주 3-5일)</option>
            <option value="VERY_ACTIVE">적극적인 활동 (주 6-7일)</option>
            <option value="SUPER_ACTIVE">매우 활동적 (격렬한 운동)</option>
          </select>
        </div>

        <div class="form-group">
          <label>목표</label>
          <select v-model="form.goalType" class="select-input" required>
            <option disabled value="">목표를 선택하세요</option>
            <option value="LOSE_WEIGHT">체중 감량</option>
            <option value="MAINTAIN">체중 유지</option>
            <option value="GAIN_WEIGHT">체중 증량</option>
          </select>
        </div>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <NnButton type="submit" block :disabled="isLoading">
          {{ isLoading ? '저장 중...' : '저장하고 시작하기' }}
        </NnButton>
      </form>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import NnCard from '../components/common/NnCard.vue';
import NnInput from '../components/common/NnInput.vue';
import NnButton from '../components/common/NnButton.vue';
import { getCurrentUser, updateCurrentUser } from '@/utils/auth.js';
import { updateUserProfile } from '../api/usersApi';

const router = useRouter();
const currentUser = ref(null);
const form = ref({
  gender: 'MALE',
  birthDate: '',
  height: null,
  weight: null,
  activityLevel: 'SEDENTARY',
  goalType: 'MAINTAIN',
});
const isLoading = ref(false);
const errorMessage = ref('');

onMounted(() => {
  currentUser.value = getCurrentUser();
  if (!currentUser.value) {
    router.push('/login');
  }
});

async function handleSubmit() {
  if (!currentUser.value) {
    errorMessage.value = '사용자 정보가 없습니다. 다시 로그인해주세요.';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    const updatedProfile = await updateUserProfile(currentUser.value.id, form.value);
    // 로컬 스토리지의 유저 정보도 업데이트
    updateCurrentUser(updatedProfile.data);
    
    // PRD 요구사항: 완료 시 /meal-plan으로 이동 (전체 리로드)
    window.location.href = '/meal-plans';
  } catch (err) {
    console.error(err);
    errorMessage.value = '프로필 업데이트 중 오류가 발생했습니다.';
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.profile-setup-page {
  max-width: 500px;
  margin: 40px auto;
  padding: 20px;
}
.header {
  text-align: center;
  margin-bottom: 24px;
}
.header h1 {
  font-size: 24px;
  font-weight: 700;
}
.header p {
  color: #6b7280;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
}
.form-group label {
  margin-bottom: 6px;
  font-weight: 500;
}
.radio-group {
  display: flex;
  gap: 16px;
}
.select-input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: white;
  font-size: 14px;
}
.error-message {
  color: #dc2626;
  font-size: 14px;
  text-align: center;
}
</style>


<template>
  <div class="profile-page">
    <div class="profile-container">
      <div class="profile-card">
        <!-- ✨ 가상 애니메이션 캐릭터 (SVG) -->
        <div class="mascot-container">
          <svg class="mascot-svg" viewBox="0 0 200 120" xmlns="http://www.w3.org/2000/svg">
            <path d="M40 120 C 40 50, 160 50, 160 120" fill="#86efac" />
            <circle cx="70" cy="90" r="5" fill="#fca5a5" opacity="0.6" />
            <circle cx="130" cy="90" r="5" fill="#fca5a5" opacity="0.6" />
            <g class="eyes">
              <circle cx="80" cy="80" r="6" fill="#1f2937" />
              <circle cx="120" cy="80" r="6" fill="#1f2937" />
            </g>
            <g class="hands">
              <circle cx="50" cy="115" r="12" fill="#4ade80" stroke="#166534" stroke-width="2" />
              <circle cx="150" cy="115" r="12" fill="#4ade80" stroke="#166534" stroke-width="2" />
            </g>
          </svg>
        </div>

        <div class="brand">
          <img src="/images/brand-logo.png" alt="남남코치" class="logo-img" />
        </div>

        <div class="header">
          <h1>프로필 설정</h1>
          <p>맞춤 식단을 위해 정보를 입력해주세요.</p>
        </div>

        <form @submit.prevent="handleSubmit" class="auth-form">
          <div class="input-group">
            <label>성별</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" v-model="form.gender" value="MALE" />
                <span class="radio-text">남성</span>
              </label>
              <label class="radio-label">
                <input type="radio" v-model="form.gender" value="FEMALE" />
                <span class="radio-text">여성</span>
              </label>
            </div>
          </div>

          <div class="row">
            <div class="input-group half">
              <label>생년월일</label>
              <NnInput type="date" v-model="form.birthDate" required />
            </div>
            <div class="input-group half">
              <label>활동량</label>
              <select v-model="form.activityLevel" class="select-input" required>
                <option value="SEDENTARY">거의 안 함</option>
                <option value="LIGHTLY_ACTIVE">가벼운 활동</option>
                <option value="MODERATE">보통 활동</option>
                <option value="VERY_ACTIVE">많은 활동</option>
                <option value="SUPER_ACTIVE">매우 많음</option>
              </select>
            </div>
          </div>

          <div class="row">
            <div class="input-group half">
              <label>키 (cm)</label>
              <NnInput type="number" v-model.number="form.height" placeholder="175" required />
            </div>
            <div class="input-group half">
              <label>체중 (kg)</label>
              <NnInput type="number" v-model.number="form.weight" placeholder="70" required />
            </div>
          </div>

          <div class="row">
             <div class="input-group">
              <label>목표 체중 (kg)</label>
              <NnInput type="number" v-model.number="form.targetWeight" placeholder="65" required />
            </div>
          </div>

          <div class="input-group">
            <label>목표</label>
            <div class="goal-options">
              <label class="goal-card" :class="{ active: form.goalType === 'LOSE_WEIGHT' }">
                <input type="radio" v-model="form.goalType" value="LOSE_WEIGHT" />
                <span>감량</span>
              </label>
              <label class="goal-card" :class="{ active: form.goalType === 'MAINTAIN' }">
                <input type="radio" v-model="form.goalType" value="MAINTAIN" />
                <span>유지</span>
              </label>
              <label class="goal-card" :class="{ active: form.goalType === 'GAIN_WEIGHT' }">
                <input type="radio" v-model="form.goalType" value="GAIN_WEIGHT" />
                <span>증량</span>
              </label>
            </div>
          </div>

          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

          <NnButton type="submit" :disabled="isLoading" class="submit-btn">
            {{ isLoading ? "저장 중..." : "시작하기" }}
          </NnButton>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import NnInput from "@/components/common/NnInput.vue";
import NnButton from "@/components/common/NnButton.vue";
import { getCurrentUser, updateCurrentUser } from "@/utils/auth.js";
import { updateUserProfile } from "@/api/usersApi";

const router = useRouter();
const currentUser = ref(null);
const form = ref({
  gender: "MALE",
  birthDate: "",
  height: null,
  weight: null,
  targetWeight: null,
  activityLevel: "SEDENTARY",
  goalType: "MAINTAIN",
});
const isLoading = ref(false);
const errorMessage = ref("");

onMounted(() => {
  currentUser.value = getCurrentUser();
  if (!currentUser.value) {
    router.push("/login");
  }
});

async function handleSubmit() {
  if (!currentUser.value) {
    errorMessage.value = "사용자 정보가 없습니다.";
    return;
  }
  isLoading.value = true;
  errorMessage.value = "";

  try {
    const updatedProfile = await updateUserProfile(currentUser.value.id, form.value);
    updateCurrentUser(updatedProfile.data);
    window.location.href = "/dashboard";
  } catch (err) {
    console.error(err);
    errorMessage.value = "프로필 업데이트 중 오류가 발생했습니다.";
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: auto; 
  /* 배경 개선 */
  background-color: #f0fdf4;
  background-image: url('/images/character.png.png');
  background-size: contain;
  background-repeat: repeat;
  background-position: center;
  padding: 40px 20px;
}

.profile-page::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
  z-index: 0;
  position: fixed; 
}

.profile-container {
  width: 100%;
  max-width: 480px;
  position: relative;
  z-index: 1;
}

.profile-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 50px 32px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.8);
  position: relative;
  margin-top: 40px;
}

/* --- SVG 마스코트 --- */
.mascot-container {
  position: absolute;
  top: -95px;
  left: 50%;
  transform: translateX(-50%);
  width: 160px;
  height: 100px;
  z-index: 2;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1));
  animation: float 3s ease-in-out infinite;
}

.mascot-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.eyes {
  animation: blink 4s infinite;
  transform-origin: center;
}

@keyframes float {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, -6px); }
}

@keyframes blink {
  0%, 48%, 52%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.1); }
}

.brand {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.logo-img {
  height: 48px;
  width: auto;
  object-fit: contain;
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 26px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
}

.header p {
  color: #4b5563;
  font-size: 15px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.input-group label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.row {
  display: flex;
  gap: 16px;
}
.half {
  flex: 1;
}

/* 라디오 & 셀렉트 & 카드 스타일은 기존 유지하되 색감 조정 */
.radio-group { display: flex; gap: 24px; }
.radio-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.select-input {
  padding: 10px; border: 1px solid #d1d5db; border-radius: 8px; height: 42px;
}

.goal-options { display: flex; gap: 12px; }
.goal-card {
  flex: 1; border: 1px solid #e5e7eb; border-radius: 8px; padding: 12px;
  text-align: center; cursor: pointer; background: rgba(255,255,255,0.5);
}
.goal-card.active {
  border-color: #047857; background-color: #ecfdf5; color: #047857; font-weight: 700;
}

.submit-btn {
  height: 50px; font-size: 16px; margin-top: 16px; border-radius: 12px;
  background: #047857; border: none;
}
.error-message { color: #ef4444; font-size: 14px; text-align: center; }
</style>
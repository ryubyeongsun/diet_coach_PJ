<template>
  <div class="profile-edit-page">
    <div class="page-header">
      <h2>내 프로필 수정</h2>
      <p>신체 정보를 최신 상태로 유지하면 더 정확한 식단을 추천받을 수 있어요.</p>
    </div>

    <NnCard class="edit-card">
      <form @submit.prevent="handleSubmit" class="edit-form">
        <!-- 기본 정보 (읽기 전용) -->
        <div class="section-title">기본 정보</div>
        <div class="form-row">
          <div class="form-group">
            <label>이름</label>
            <NnInput :model-value="currentUser?.name" readonly disabled />
          </div>
          <div class="form-group">
            <label>이메일</label>
            <NnInput :model-value="currentUser?.email" readonly disabled />
          </div>
        </div>

        <div class="divider"></div>

        <!-- 신체 정보 (수정 가능) -->
        <div class="section-title">신체 정보</div>
        
        <div class="form-group">
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

        <div class="form-row">
          <div class="form-group half">
            <label>생년월일</label>
            <NnInput type="date" v-model="form.birthDate" required />
          </div>
          <div class="form-group half">
            <label>활동량</label>
            <select v-model="form.activityLevel" class="select-input" required>
              <option value="SEDENTARY">거의 운동 안 함</option>
              <option value="LIGHTLY_ACTIVE">가벼운 활동 (주 1-3회)</option>
              <option value="MODERATE">보통 활동 (주 3-5회)</option>
              <option value="VERY_ACTIVE">많은 활동 (주 6-7회)</option>
              <option value="SUPER_ACTIVE">매우 많음 (격렬한 운동)</option>
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group half">
            <label>키 (cm)</label>
            <NnInput type="number" v-model.number="form.height" placeholder="175" required />
          </div>
          <div class="form-group half">
            <label>체중 (kg)</label>
            <NnInput type="number" v-model.number="form.weight" placeholder="70" required />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>목표 체중 (kg)</label>
            <NnInput type="number" v-model.number="form.targetWeight" placeholder="65" required />
          </div>
        </div>

        <div class="form-group">
          <label>목표</label>
          <div class="goal-options">
            <label class="goal-card" :class="{ active: form.goalType === 'LOSE_WEIGHT' }">
              <input type="radio" v-model="form.goalType" value="LOSE_WEIGHT" />
              <span>체중 감량</span>
            </label>
            <label class="goal-card" :class="{ active: form.goalType === 'MAINTAIN' }">
              <input type="radio" v-model="form.goalType" value="MAINTAIN" />
              <span>체중 유지</span>
            </label>
            <label class="goal-card" :class="{ active: form.goalType === 'GAIN_WEIGHT' }">
              <input type="radio" v-model="form.goalType" value="GAIN_WEIGHT" />
              <span>체중 증량</span>
            </label>
          </div>
        </div>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <div class="btn-group">
          <NnButton variant="secondary" @click="router.back()" type="button">취소</NnButton>
          <NnButton type="submit" :disabled="isLoading">
            {{ isLoading ? "저장 중..." : "수정사항 저장" }}
          </NnButton>
        </div>
      </form>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import NnInput from "@/components/common/NnInput.vue";
import NnButton from "@/components/common/NnButton.vue";
import NnCard from "@/components/common/NnCard.vue";
import { getCurrentUser, updateCurrentUser } from "@/utils/auth.js";
import { fetchUserProfile, updateUserProfile } from "@/api/usersApi";

const router = useRouter();
const currentUser = ref(null);
const isLoading = ref(false);
const errorMessage = ref("");

const form = ref({
  gender: "MALE",
  birthDate: "",
  height: null,
  weight: null,
  targetWeight: null,
  activityLevel: "SEDENTARY",
  goalType: "MAINTAIN",
});

onMounted(async () => {
  currentUser.value = getCurrentUser();
  if (!currentUser.value) {
    router.push("/login");
    return;
  }

  // 기존 프로필 데이터 불러오기
  try {
    const response = await fetchUserProfile(currentUser.value.id);
    if (response.success) {
      const data = response.data;
      form.value = {
        gender: data.gender || "MALE",
        birthDate: data.birthDate || "",
        height: data.height,
        weight: data.weight,
        targetWeight: data.targetWeight,
        activityLevel: data.activityLevel || "SEDENTARY",
        goalType: data.goalType || "MAINTAIN",
      };
    }
  } catch (error) {
    console.error("Failed to fetch profile:", error);
    errorMessage.value = "프로필 정보를 불러오는데 실패했습니다.";
  }
});

async function handleSubmit() {
  if (!currentUser.value) return;
  
  isLoading.value = true;
  errorMessage.value = "";

  try {
    const response = await updateUserProfile(currentUser.value.id, form.value);
    
    // 로컬 스토리지 정보도 업데이트 (필요한 경우)
    // updateCurrentUser(...) 는 보통 Auth 정보를 갱신하지만, 
    // 프로필 정보(키/몸무게 등)가 Auth 객체에 포함되어 있다면 갱신 필요.
    // 여기서는 name, email 등 기본 정보는 안 바뀌지만 혹시 모르니 갱신 로직을 둠.
    
    alert("프로필이 성공적으로 수정되었습니다.");
    // router.push("/dashboard"); // or stay here
  } catch (err) {
    console.error(err);
    errorMessage.value = "프로필 수정 중 오류가 발생했습니다.";
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.profile-edit-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}
.page-header p {
  color: #6b7280;
  font-size: 14px;
}

.edit-card {
  padding: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #374151;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 4px solid #047857;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  gap: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  margin-bottom: 6px;
}

.divider {
  height: 1px;
  background-color: #e5e7eb;
  margin: 8px 0;
}

/* 라디오/셀렉트 스타일 (ProfileSetupPage와 유사하게) */
.radio-group { display: flex; gap: 24px; }
.radio-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.select-input {
  padding: 10px; border: 1px solid #d1d5db; border-radius: 8px; height: 42px; width: 100%;
}

.goal-options { display: flex; gap: 12px; }
.goal-card {
  flex: 1; border: 1px solid #e5e7eb; border-radius: 8px; padding: 12px;
  text-align: center; cursor: pointer; transition: all 0.2s;
}
.goal-card.active {
  border-color: #047857; background-color: #ecfdf5; color: #047857; font-weight: 700;
}

.error-message { color: #ef4444; font-size: 14px; text-align: center; }

.btn-group {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style>

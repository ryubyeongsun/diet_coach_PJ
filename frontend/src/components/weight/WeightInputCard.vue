<template>
  <div class="weight-input-card">
    <h3 class="card-title">오늘의 체중 ({{ todayDate }})</h3>
    <div class="input-wrapper">
      <input 
        v-model="inputWeight" 
        type="number" 
        step="0.1"
        class="weight-input"
        placeholder="예: 75.2"
        @keyup.enter="saveWeight"
      />
      <span class="unit">kg</span>
    </div>
    <button 
      class="save-button" 
      @click="saveWeight"
      :disabled="!inputWeight || isSaving"
    >
      <span v-if="!isSaving">✓ 저장하기</span>
      <span v-else>저장 중...</span>
    </button>
    <transition name="success">
      <div v-if="showSuccess" class="success-message">
        ✅ 체중이 기록되었습니다!
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { upsertWeight } from '../../api/weightApi';
import { getCurrentUser } from '../../utils/auth';
import { format } from 'date-fns';

const emit = defineEmits(['weight-saved']);

const inputWeight = ref('');
const isSaving = ref(false);
const showSuccess = ref(false);
const currentUser = getCurrentUser();

const todayDate = computed(() => {
  return format(new Date(), "MM월 dd일");
});

async function saveWeight() {
  if (!inputWeight.value) return;
  if (!currentUser) {
    alert("로그인이 필요합니다.");
    return;
  }
  
  isSaving.value = true;
  try {
    await upsertWeight(currentUser.id, {
      weight: parseFloat(inputWeight.value),
      recordDate: format(new Date(), "yyyy-MM-dd")
    });
    
    showSuccess.value = true;
    emit('weight-saved');
    
    setTimeout(() => {
      showSuccess.value = false;
      inputWeight.value = '';
    }, 2000);
  } catch (error) {
    console.error('체중 저장 실패:', error);
    alert('체중 저장에 실패했습니다.');
  } finally {
    isSaving.value = false;
  }
}
</script>

<style scoped>
.weight-input-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  border: 2px solid #E8F5E9;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #2E7D32;
  margin-bottom: 20px;
  margin-top: 0;
}

.input-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.weight-input {
  width: 100%;
  font-size: 32px;
  font-weight: 600;
  text-align: center;
  border: 2px solid #A5D6A7;
  border-radius: 12px;
  padding: 16px 60px 16px 16px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.weight-input:focus {
  outline: none;
  border-color: #66BB6A;
  box-shadow: 0 0 0 4px rgba(102, 187, 106, 0.1);
}

.unit {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  font-weight: 600;
  color: #7CB342;
}

.save-button {
  width: 100%;
  background: linear-gradient(135deg, #66BB6A 0%, #4CAF50 100%);
  color: white;
  border-radius: 12px;
  padding: 14px 28px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.save-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.save-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.success-message {
  margin-top: 12px;
  padding: 12px;
  background: #E8F5E9;
  border-radius: 8px;
  color: #2E7D32;
  text-align: center;
  font-weight: 600;
}

.success-enter-active, .success-leave-active {
  transition: all 0.3s ease;
}

.success-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.success-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>

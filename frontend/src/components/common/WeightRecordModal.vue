<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h2>오늘 체중 기록</h2>
        <button class="close-btn" @click="close">&times;</button>
      </div>
      <form @submit.prevent="handleSave">
        <p>오늘 ({{ todayDate }}) 체중을 기록합니다.</p>
        <div class="input-group">
          <NnInput
            ref="inputRef"
            v-model.number="weight"
            type="number"
            step="0.1"
            placeholder="예: 75.2"
          />
          <span class="unit">kg</span>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <NnButton type="button" variant="secondary" @click="close">취소</NnButton>
          <NnButton type="submit" :disabled="isSaving">
            {{ isSaving ? '저장 중...' : '저장' }}
          </NnButton>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { format } from 'date-fns';
import { upsertWeight } from '../../api/weightApi';
import { getCurrentUser } from '../../utils/auth';
import NnInput from './NnInput.vue';
import NnButton from './NnButton.vue';

const props = defineProps({
  isOpen: Boolean,
});
const emit = defineEmits(['close', 'saved']);

const weight = ref(null);
const isSaving = ref(false);
const error = ref('');
const inputRef = ref(null); // for autofocus

const todayDate = format(new Date(), 'yyyy-MM-dd');

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    // Reset state when modal opens
    weight.value = null;
    error.value = '';
    // Autofocus the input field
    setTimeout(() => {
      inputRef.value?.focus();
    }, 100);
  }
});

const close = () => {
  if (isSaving.value) return;
  emit('close');
};

async function handleSave() {
  const currentUser = getCurrentUser();

  if (!weight.value || weight.value <= 0) {
    error.value = '올바른 체중을 입력해주세요.';
    return;
  }
  if (!currentUser?.id) {
    error.value = '사용자 정보를 찾을 수 없습니다. 다시 로그인 해주세요.';
    return;
  }

  isSaving.value = true;
  error.value = '';

  try {
    await upsertWeight(currentUser.id, {
      recordDate: todayDate,
      weight: weight.value,
    });
    alert('체중이 성공적으로 기록되었습니다.');
    emit('saved');
  } catch (err) {
    console.error('Failed to save weight from modal:', err);
    error.value = '저장에 실패했습니다. 잠시 후 다시 시도해주세요.';
  } finally {
    isSaving.value = false;
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.2);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.modal-header h2 {
  margin: 0;
  font-size: 18px;
}
.close-btn {
  border: none;
  background: none;
  font-size: 24px;
  cursor: pointer;
  color: #9ca3af;
}
.input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0;
}
.unit {
  font-size: 16px;
  color: #4b5563;
}
.error-text {
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 16px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>

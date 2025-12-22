<script setup>
import { ref, watch, computed } from 'vue';
import { upsertWeight, deleteWeight } from '../../api/weightApi';
import { getCurrentUser } from '../../utils/auth';
import NnInput from '../common/NnInput.vue';
import NnButton from '../common/NnButton.vue';

const props = defineProps({
  isOpen: Boolean,
  record: Object, // An existing record to edit
  date: String,   // A date for which to add a new record
});
const emit = defineEmits(['close', 'updated', 'deleted']);

const editableWeight = ref(null);
const isSaving = ref(false);
const error = ref('');
const inputRef = ref(null);
const currentUser = getCurrentUser();

const isEditMode = computed(() => !!props.record);

const displayDate = computed(() => props.record?.recordDate || props.date);

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    editableWeight.value = props.record?.weight || null;
    error.value = '';
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
  if (!editableWeight.value || editableWeight.value <= 0) {
    error.value = '올바른 체중을 입력해주세요.';
    return;
  }
  if (!currentUser?.id) return;

  isSaving.value = true;
  error.value = '';
  try {
    await upsertWeight(currentUser.id, {
      recordDate: displayDate.value,
      weight: editableWeight.value,
    });
    emit('updated'); // Use a generic 'updated' event for both add and edit
    close();
  } catch (err) {
    error.value = '저장에 실패했습니다.';
  } finally {
    isSaving.value = false;
  }
}

async function handleDelete() {
  if (!isEditMode.value || !confirm('정말로 이 기록을 삭제하시겠습니까?')) return;
  if (!currentUser?.id || !props.record?.id) return;

  isSaving.value = true;
  error.value = '';
  try {
    await deleteWeight(currentUser.id, props.record.id);
    emit('deleted');
    close();
  } catch (err) {
    error.value = '삭제에 실패했습니다.';
  } finally {
    isSaving.value = false;
  }
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="close">
    <div class="modal-content" v-if="displayDate">
      <div class="modal-header">
        <h2>{{ isEditMode ? '체중 수정/삭제' : '체중 기록' }}</h2>
        <button class="close-btn" @click="close">&times;</button>
      </div>
      <form @submit.prevent="handleSave">
        <p>{{ displayDate }}의 체중을 기록합니다.</p>
        <div class="input-group">
          <NnInput
            ref="inputRef"
            v-model.number="editableWeight"
            type="number"
            step="0.1"
          />
          <span class="unit">kg</span>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <NnButton v-if="isEditMode" type="button" variant="danger" @click="handleDelete" :disabled="isSaving">삭제</NnButton>
          <div class="spacer"></div>
          <NnButton type="button" variant="secondary" @click="close">취소</NnButton>
          <NnButton type="submit" :disabled="isSaving">
            {{ isSaving ? '저장 중...' : (isEditMode ? '수정' : '저장') }}
          </NnButton>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background-color: rgba(0, 0, 0, 0.6); display: flex;
  justify-content: center; align-items: center; z-index: 1000;
}
.modal-content {
  background: white; padding: 24px; border-radius: 12px;
  width: 90%; max-width: 400px; box-shadow: 0 5px 15px rgba(0,0,0,0.2);
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.modal-header h2 { margin: 0; font-size: 18px; }
.close-btn {
  border: none; background: none; font-size: 24px;
  cursor: pointer; color: #9ca3af;
}
.input-group {
  display: flex; align-items: center; gap: 8px; margin: 16px 0;
}
.unit { font-size: 16px; color: #4b5563; }
.error-text { color: #ef4444; font-size: 13px; margin-bottom: 16px; }
.modal-actions {
  display: flex; justify-content: flex-end; align-items: center;
  gap: 12px; margin-top: 24px;
}
.spacer { flex-grow: 1; }
</style>

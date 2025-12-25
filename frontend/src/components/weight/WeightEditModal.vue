<template>
  <transition name="modal">
    <div v-if="isOpen" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3 class="modal-title">체중 수정/삭제</h3>
        <p class="modal-date">{{ formattedDate }}</p>
        
        <div class="input-wrapper">
          <input 
            v-model="editWeight" 
            type="number" 
            step="0.1"
            class="modal-input"
            placeholder="체중 입력"
            @keyup.enter="updateRecord"
          />
          <span class="unit">kg</span>
        </div>
        
        <div class="modal-buttons">
          <button class="modal-btn btn-cancel" @click="closeModal">
            취소
          </button>
          <button v-if="currentWeight" class="modal-btn btn-delete" @click="confirmDelete">
            삭제
          </button>
          <button class="modal-btn btn-update" @click="updateRecord">
            {{ currentWeight ? '수정' : '저장' }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { upsertWeight, deleteWeight } from '../../api/weightApi';
import { getCurrentUser } from '../../utils/auth';

const props = defineProps({
  isOpen: Boolean,
  selectedDate: String, // YYYY-MM-DD
  currentWeight: Number
});

const emit = defineEmits(['close', 'updated', 'deleted']);

const editWeight = ref(props.currentWeight);
const currentUser = getCurrentUser();

watch(() => props.currentWeight, (newVal) => {
  editWeight.value = newVal;
});

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    editWeight.value = props.currentWeight;
  }
});

const formattedDate = computed(() => {
  if (!props.selectedDate) return '';
  const [y, m, d] = props.selectedDate.split('-');
  return `${y}년 ${parseInt(m)}월 ${parseInt(d)}일`;
});

function closeModal() {
  emit('close');
}

async function updateRecord() {
  if (!editWeight.value) {
    alert('체중을 입력해주세요.');
    return;
  }
  
  try {
    await upsertWeight(currentUser.id, {
      recordDate: props.selectedDate,
      weight: parseFloat(editWeight.value)
    });
    
    emit('updated');
  } catch (error) {
    console.error('체중 수정 실패:', error);
    alert('체중 수정에 실패했습니다.');
  }
}

async function confirmDelete() {
  if (confirm('정말 삭제하시겠습니까?')) {
    try {
      await deleteWeight(currentUser.id, props.selectedDate);
      emit('deleted');
    } catch (error) {
      console.error('체중 삭제 실패:', error);
      alert('체중 삭제에 실패했습니다.');
    }
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 24px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #2E7D32;
  margin-bottom: 8px;
  margin-top: 0;
}

.modal-date {
  font-size: 14px;
  color: #7CB342;
  margin-bottom: 24px;
}

.input-wrapper {
  position: relative;
  margin-bottom: 24px;
}

.modal-input {
  width: 100%;
  font-size: 28px;
  font-weight: 600;
  text-align: center;
  border: 2px solid #A5D6A7;
  border-radius: 12px;
  padding: 16px 60px 16px 16px;
  box-sizing: border-box;
  transition: all 0.3s ease;
  font-family: inherit;
}

.modal-input:focus {
  outline: none;
  border-color: #66BB6A;
  box-shadow: 0 0 0 4px rgba(102, 187, 106, 0.1);
}

.unit {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  font-weight: 600;
  color: #7CB342;
}

.modal-buttons {
  display: flex;
  gap: 12px;
}

.modal-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel {
  background: #ECEFF1;
  color: #546E7A;
}

.btn-delete {
  background: #EF5350;
  color: white;
}

.btn-update {
  background: #66BB6A;
  color: white;
}

.modal-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from, .modal-leave-to {
  opacity: 0;
}

.modal-enter-active .modal-content,
.modal-leave-active .modal-content {
  transition: transform 0.3s ease;
}

.modal-enter-from .modal-content {
  transform: translateY(20px);
}

.modal-leave-to .modal-content {
  transform: translateY(20px);
}
</style>
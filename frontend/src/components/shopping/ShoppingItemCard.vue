<template>
  <div 
    class="shopping-card" 
    :class="{ 'is-checked': isChecked, 'has-product': !!item.product }"
    @click="$emit('toggle')"
  >
    <!-- 체크박스 (이미지 위 플로팅) -->
    <div class="checkbox-overlay">
      <div class="custom-checkbox" :class="{ checked: isChecked }">
        <span v-if="isChecked">✔</span>
      </div>
    </div>

    <!-- 상품 이미지 영역 -->
    <div class="image-area">
      <img 
        v-if="item.product && item.product.imageUrl" 
        :src="item.product.imageUrl" 
        :alt="item.product.title" 
      />
      <div v-else class="placeholder-img">
        <span>이미지 없음</span>
      </div>
      
      <div class="ingredient-badge">
        {{ item.ingredientName }} {{ item.totalGram ? `(${item.totalGram}g)` : '' }}
      </div>
    </div>

    <!-- 정보 영역 -->
    <div class="info-area">
      <div v-if="item.product">
        <div class="product-title" :title="item.product.title">
          {{ item.product.title }}
        </div>
        <div class="price-row">
          <span class="price">{{ item.product.price.toLocaleString() }}원</span>
          <span class="mall">{{ item.product.mallName }}</span>
        </div>
        
        <button 
          class="add-cart-btn" 
          @click.stop="$emit('add-to-cart', item.product)"
        >
          🛒 담기
        </button>
      </div>
      
      <div v-else class="empty-state">
        <p>추천 상품이 없습니다.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true,
  },
  isChecked: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['toggle', 'add-to-cart']);
</script>

<style scoped>
.shopping-card {
  display: flex;
  flex-direction: column;
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 100%;
}

.shopping-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border-color: #d1fae5;
}

.shopping-card.is-checked {
  opacity: 0.8;
  border-color: #10b981;
}

/* 체크박스 */
.checkbox-overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
}

.custom-checkbox {
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid #d1d5db;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  transition: all 0.2s;
  cursor: pointer;
}

.shopping-card.is-checked .custom-checkbox {
  background-color: #10b981;
  border-color: #10b981;
}

/* 이미지 영역 */
.image-area {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 Aspect Ratio */
  background-color: #f9fafb;
}

.image-area img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 12px;
}

.ingredient-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

/* 정보 영역 */
.info-area {
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-title {
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
  line-height: 1.4;
  height: 2.8em; /* 2 lines */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.price {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.mall {
  font-size: 11px;
  color: #6b7280;
  background-color: #f3f4f6;
  padding: 2px 4px;
  border-radius: 2px;
}

.add-cart-btn {
  width: 100%;
  padding: 10px;
  background-color: #047857;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: auto;
}

.add-cart-btn:hover {
  background-color: #065f46;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
  min-height: 80px;
}
</style>

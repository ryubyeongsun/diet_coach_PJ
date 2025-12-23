<template>
  <div 
    class="shopping-card" 
    :class="{ 
      'is-checked': isChecked, 
      'has-product': !!item.product,
      'is-purchased': isPurchased 
    }"
    @click="!isPurchased && $emit('toggle')"
  >
    <!-- 구매 완료 뱃지 -->
    <div v-if="isPurchased" class="purchased-overlay">
      <span>구매 완료</span>
    </div>

    <!-- 체크박스 -->
    <div class="checkbox-container">
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
        <span>No Img</span>
      </div>
    </div>

    <!-- 정보 영역 -->
    <div class="info-area">
      <div class="info-header">
        <span class="ingredient-name">{{ item.ingredientName }}</span>
        <span v-if="item.totalGram" class="ingredient-gram">{{ item.totalGram }}g</span>
      </div>

      <div v-if="item.product" class="product-details">
        <div class="product-title" :title="item.product.title">
          {{ item.product.title }}
        </div>
        <div class="price-row">
          <span class="price">{{ item.product.price.toLocaleString() }}원</span>
          <span class="mall">{{ item.product.mallName }}</span>
        </div>
      </div>
      
      <div v-else class="empty-state">
        <span class="no-product-text">상품 정보 없음</span>
      </div>
    </div>

    <!-- 링크 버튼 -->
    <div class="action-area" v-if="item.product">
       <a 
          v-if="item.product.productUrl"
          :href="item.product.productUrl"
          target="_blank"
          class="add-cart-btn" 
          title="상품 보러가기"
          @click.stop
        >
          🔗
        </a>
    </div>
  </div>
</template>

<script setup>
defineProps({
  item: { type: Object, required: true },
  isChecked: { type: Boolean, default: false },
  isPurchased: { type: Boolean, default: false }
});
defineEmits(['toggle']);
</script>

<style scoped>
.shopping-card {
  display: flex; flex-direction: row; align-items: center;
  background-color: white; border: 1px solid #e5e7eb;
  border-radius: 8px; overflow: hidden; cursor: pointer;
  transition: all 0.2s ease; height: 80px;
  position: relative;
}
.shopping-card:hover { transform: translateY(-2px); box-shadow: 0 4px 6px rgba(0,0,0,0.1); border-color: #d1fae5; }
.shopping-card.is-checked { background-color: #f0fdf4; border-color: #10b981; }

.shopping-card.is-purchased {
  opacity: 0.6;
  filter: grayscale(0.8);
  cursor: default;
  background-color: #f3f4f6;
  border-color: #d1d5db;
  pointer-events: none;
}

.purchased-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.purchased-overlay span {
  background: #374151;
  color: white;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.checkbox-container { padding: 0 12px; }
.custom-checkbox {
  width: 20px; height: 20px; background: white; border: 2px solid #d1d5db;
  border-radius: 4px; display: flex; align-items: center; justify-content: center;
  color: white; font-size: 12px; transition: all 0.2s;
}
.shopping-card.is-checked .custom-checkbox { background-color: #10b981; border-color: #10b981; }
.image-area { width: 80px; height: 80px; flex-shrink: 0; background-color: #f9fafb; }
.image-area img { width: 100%; height: 100%; object-fit: cover; }
.placeholder-img { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #9ca3af; font-size: 11px; }
.info-area { flex: 1; padding: 8px 12px; display: flex; flex-direction: column; justify-content: center; min-width: 0; gap: 4px; }
.info-header { display: flex; align-items: center; gap: 6px; }
.ingredient-name { font-weight: 700; font-size: 14px; color: #111827; }
.ingredient-gram { font-size: 12px; color: #6b7280; background-color: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
.product-title { font-size: 13px; color: #4b5563; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.price-row { display: flex; align-items: center; gap: 8px; }
.price { font-size: 14px; font-weight: 700; color: #059669; }
.action-area { padding: 0 16px; }
.add-cart-btn { width: 36px; height: 36px; border-radius: 50%; background-color: #f3f4f6; display: flex; align-items: center; justify-content: center; text-decoration: none; }
</style>
<template>
  <div class="shopping-item-card">
    <div class="ingredient-info">
      <h3 class="ingredient-name">{{ item.ingredientName }}</h3>
      <p v-if="item.totalGram" class="ingredient-amount">
        총 필요량: {{ item.totalGram }}g
      </p>
    </div>

    <div v-if="item.product" class="product-card">
      <div class="product-image">
        <img :src="item.product.imageUrl" :alt="item.product.name" />
      </div>
      <div class="product-details">
        <p class="product-name">{{ item.product.name }}</p>
        <p class="product-price">{{ item.product.price.toLocaleString() }}원</p>
      </div>
      <a
        :href="item.product.productUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="buy-button"
      >
        구매하기
      </a>
    </div>

    <div v-else class="no-product-card">
      <p>추천 상품을 찾을 수 없습니다.</p>
      <button class="buy-button" disabled>구매 불가</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true,
  },
});
</script>

<style scoped>
.shopping-item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  gap: 16px;
}

.ingredient-info {
  flex: 1;
  min-width: 120px;
}

.ingredient-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  text-transform: capitalize;
}

.ingredient-amount {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.product-card,
.no-product-card {
  flex: 3;
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #f9fafb;
  padding: 12px;
  border-radius: 6px;
}

.no-product-card {
  color: #6b7280;
  font-size: 14px;
}

.product-image {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #f3f4f6;
}

.product-details {
  flex-grow: 1;
  overflow: hidden;
}

.product-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-price {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.buy-button {
  display: inline-block;
  padding: 8px 16px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  text-align: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.buy-button:hover {
  background-color: #1d4ed8;
}

.buy-button[disabled] {
  background-color: #d1d5db;
  cursor: not-allowed;
}
</style>

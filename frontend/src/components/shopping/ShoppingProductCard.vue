<script setup>
import NnButton from "../common/NnButton.vue";

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(["add-to-cart"]);
</script>

<template>
  <div class="product-card">
    <div class="product-card__thumb">
      <img
        v-if="product.imageUrl"
        :src="product.imageUrl"
        :alt="product.name"
      />
      <div v-else class="product-card__placeholder">🛒</div>
    </div>

    <div class="product-card__body">
      <p class="product-card__title">{{ product.name }}</p>

      <div class="product-card__details">
        <p class="product-card__price">
          {{ product.price?.toLocaleString() }}원
        </p>
        <p v-if="product.mallName" class="product-card__mall">
          {{ product.mallName }}
        </p>
        <p class="product-card__gram-info">
          <span v-if="product.gramPerUnit"
            >약 {{ product.gramPerUnit.toLocaleString() }}g</span
          >
          <span v-if="product.gramPerUnit && product.pricePer100g"> · </span>
          <span v-if="product.pricePer100g"
            >100g당 {{ product.pricePer100g.toLocaleString() }}원</span
          >
        </p>
      </div>

      <div class="product-card__actions">
        <NnButton
          size="sm"
          variant="outline"
          @click="emit('add-to-cart', product)"
        >
          담기
        </NnButton>
        <a
          :href="product.productUrl"
          target="_blank"
          rel="noopener noreferrer"
          v-if="product.productUrl"
        >
          <NnButton size="sm">상세 보기</NnButton>
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}

.product-card__thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: #f3f4f6;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-card__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-card__placeholder {
  font-size: 32px;
}

.product-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  line-height: 1.4;
}

.product-card__details {
  margin-top: 6px;
  font-size: 13px;
  color: #374151;
}
.product-card__details p {
  margin: 0 0 2px;
}

.product-card__price {
  font-size: 14px;
  font-weight: 700;
}

.product-card__mall {
  color: #6b7280;
  font-size: 12px;
}

.product-card__gram-info {
  color: #6b7280;
  font-size: 12px;
}

.product-card__actions {
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>

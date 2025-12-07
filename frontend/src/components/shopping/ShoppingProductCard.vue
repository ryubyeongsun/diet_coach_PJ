<script setup>
import NnButton from '../common/NnButton.vue';

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
});
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
      <div class="product-card__title">
        {{ product.name }}
      </div>

      <div class="product-card__meta">
        <div class="product-card__price">
          <span class="product-card__price-main">
            {{ product.price?.toLocaleString() }}원
          </span>
          <span
            v-if="product.pricePer100g"
            class="product-card__price-sub"
          >
            (100g당 {{ product.pricePer100g.toLocaleString() }}원)
          </span>
        </div>
        <div class="product-card__mall">
          {{ product.mallName || '11번가' }}
        </div>
      </div>

      <div class="product-card__actions">
        <a
          v-if="product.productUrl"
          :href="product.productUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          <NnButton size="small">
            상품 보러가기
          </NnButton>
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}

.product-card__thumb {
  width: 64px;
  height: 64px;
  border-radius: 12px;
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
  font-size: 24px;
}

.product-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.product-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  font-size: 12px;
}

.product-card__price-main {
  font-weight: 700;
  color: #111827;
}

.product-card__price-sub {
  margin-left: 4px;
  color: #6b7280;
}

.product-card__mall {
  color: #6b7280;
}

.product-card__actions {
  margin-top: 6px;
}
</style>

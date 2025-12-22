<template>
  <div class="page">
    <header class="page__header">
      <h1>장바구니</h1>
      <p>담아둔 상품 목록을 확인하고 결제를 진행하세요.</p>
    </header>

    <NnCard>
      <div v-if="cartItems.length === 0" class="empty-cart">
        <p>텅 비었습니다!</p>
        <NnButton @click="$router.push('/shopping')">쇼핑 계속하기</NnButton>
      </div>

      <div v-else>
        <ul class="cart-list">
          <li
            v-for="item in cartItems"
            :key="item.productCode"
            class="cart-item"
          >
            <img
              :src="item.imageUrl"
              :alt="item.name"
              class="cart-item__thumb"
              v-if="item.imageUrl"
            />
            <div v-else class="cart-item__thumb-placeholder">🛒</div>
            <div class="cart-item__info">
              <span class="cart-item__name">{{ item.name }}</span>
              <span class="cart-item__price"
                >{{ item.price?.toLocaleString() }}원</span
              >
            </div>
            <div class="cart-item__quantity">수량: {{ item.quantity }}</div>
            <div class="cart-item__subtotal">
              {{ (item.price * item.quantity).toLocaleString() }}원
            </div>
          </li>
        </ul>
        <hr class="divider" />
        <div class="cart-summary">
          <strong>총 합계:</strong>
          <span>{{ totalAmount.toLocaleString() }}원</span>
        </div>
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { globalState } from "../utils/globalState";
import NnCard from "../components/common/NnCard.vue";
import NnButton from "../components/common/NnButton.vue";

const cartItems = computed(() => globalState.cart);

const totalAmount = computed(() => {
  return cartItems.value.reduce(
    (total, item) => total + item.price * item.quantity,
    0,
  );
});
</script>

<style scoped>
.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page__header h1 {
  font-size: 24px;
  font-weight: 700;
}
.page__header p {
  color: #6b7280;
}
.empty-cart {
  text-align: center;
  padding: 40px;
}
.empty-cart p {
  margin-bottom: 16px;
  font-size: 16px;
}
.cart-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.cart-item__thumb,
.cart-item__thumb-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background-color: #f3f4f6;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cart-item__thumb {
  object-fit: cover;
}
.cart-item__thumb-placeholder {
  font-size: 24px;
}
.cart-item__info {
  flex-grow: 1;
}
.cart-item__name {
  font-weight: 600;
  display: block;
}
.cart-item__price {
  font-size: 14px;
  color: #4b5563;
}
.cart-item__quantity {
  font-size: 14px;
}
.cart-item__subtotal {
  font-weight: 700;
  width: 100px;
  text-align: right;
}
.divider {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 20px 0;
}
.cart-summary {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  font-size: 18px;
}
</style>

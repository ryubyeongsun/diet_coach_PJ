import { reactive } from "vue";

export const globalState = reactive({
  isLoading: false,
  error: null,
  cart: [],
  isWeightModalOpen: false,
});

export function setWeightModalOpen(isOpen) {
  globalState.isWeightModalOpen = isOpen;
}

export function setLoading(status) {
  globalState.isLoading = status;
}

export function setError(message) {
  globalState.error = message;
  // 5초 후에 자동으로 에러 메시지 초기화
  setTimeout(() => {
    globalState.error = null;
  }, 5000);
}

export function addToCart(product) {
  const existingItem = globalState.cart.find(
    (item) => item.productCode === product.externalId,
  );

  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    globalState.cart.push({
      productCode: product.externalId,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrl,
      quantity: 1,
    });
  }
  alert(`${product.name} 상품이 장바구니에 추가되었습니다.`);
}

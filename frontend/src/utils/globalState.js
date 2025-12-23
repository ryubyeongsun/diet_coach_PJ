import { reactive, watch } from "vue";

const CART_STORAGE_KEY = "nncoach_cart";
const PURCHASED_ID_STORAGE_KEY = "nncoach_purchased_ids";
const PURCHASED_ITEMS_STORAGE_KEY = "nncoach_purchased_items";

// Load initial data
const savedCart = localStorage.getItem(CART_STORAGE_KEY);
const initialCart = savedCart ? JSON.parse(savedCart) : [];

const savedPurchasedIds = localStorage.getItem(PURCHASED_ID_STORAGE_KEY);
const initialPurchasedIds = savedPurchasedIds ? JSON.parse(savedPurchasedIds) : [];

const savedPurchasedItems = localStorage.getItem(PURCHASED_ITEMS_STORAGE_KEY);
const initialPurchasedItems = savedPurchasedItems ? JSON.parse(savedPurchasedItems) : [];

export const globalState = reactive({
  isLoading: false,
  error: null,
  cart: initialCart,
  confirmed: new Set(initialPurchasedIds), 
  purchasedItems: initialPurchasedItems, // Full objects for ledger
  isWeightModalOpen: false,
});

// Sync watchers
watch(() => globalState.cart, (newVal) => {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(newVal));
}, { deep: true });

watch(() => globalState.confirmed, (newVal) => {
  localStorage.setItem(PURCHASED_ID_STORAGE_KEY, JSON.stringify([...newVal]));
}, { deep: true });

watch(() => globalState.purchasedItems, (newVal) => {
  localStorage.setItem(PURCHASED_ITEMS_STORAGE_KEY, JSON.stringify(newVal));
}, { deep: true });

export function setWeightModalOpen(isOpen) {
  globalState.isWeightModalOpen = isOpen;
}

export function setLoading(status) {
  globalState.isLoading = status;
}

export function setError(message) {
  globalState.error = message;
  setTimeout(() => {
    globalState.error = null;
  }, 5000);
}

export function addToCart(product) {
  const existingItem = globalState.cart.find(
    (item) => item.productCode === product.externalId
  );

  if (existingItem) {
    existingItem.quantity = 1; 
  } else {
    globalState.cart.push({
      productCode: product.externalId,
      name: product.title || product.name,
      ingredientName: product.ingredientName, // Added
      price: product.price,
      imageUrl: product.imageUrl,
      productUrl: product.productUrl,
      quantity: 1,
    });
  }
}

export function removeFromCart(productCode) {
  const index = globalState.cart.findIndex(item => item.productCode === productCode);
  if (index !== -1) {
    globalState.cart.splice(index, 1);
  }
}

export function confirmCurrentCart() {
  // Move all cart item IDs to confirmed set
  globalState.cart.forEach(item => {
    globalState.confirmed.add(item.productCode);
  });
  // Clear cart
  globalState.cart = [];
}

export function isPurchased(productCode) {
  return globalState.confirmed.has(productCode);
}

export function clearCart() {
  globalState.cart = [];
}
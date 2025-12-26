import { reactive, watch } from "vue";
import { getCurrentUser } from "./auth";

const CART_STORAGE_KEY = "nncoach_cart";
const PURCHASED_ID_STORAGE_KEY = "nncoach_purchased_ids";
const PURCHASED_ITEMS_STORAGE_KEY = "nncoach_purchased_items";

function getStorageKey(base) {
  const user = getCurrentUser();
  const suffix = user?.id ? `_${user.id}` : "_guest";
  return `${base}${suffix}`;
}

function loadFromStorage(key, fallback) {
  const raw = localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

// Load initial data
const initialCart = loadFromStorage(getStorageKey(CART_STORAGE_KEY), []);
const initialPurchasedIds = loadFromStorage(getStorageKey(PURCHASED_ID_STORAGE_KEY), []);
const initialPurchasedItems = loadFromStorage(getStorageKey(PURCHASED_ITEMS_STORAGE_KEY), []);

export const globalState = reactive({
  isLoading: false,
  error: null,
  cart: initialCart,
  confirmed: new Set(initialPurchasedIds), 
  purchasedItems: initialPurchasedItems, // Full objects for ledger
  isWeightModalOpen: false,
});

watch(
  () => globalState.cart,
  (value) => {
    localStorage.setItem(getStorageKey(CART_STORAGE_KEY), JSON.stringify(value));
  },
  { deep: true },
);

watch(
  () => Array.from(globalState.confirmed),
  (value) => {
    localStorage.setItem(getStorageKey(PURCHASED_ID_STORAGE_KEY), JSON.stringify(value));
  },
  { deep: true },
);

watch(
  () => globalState.purchasedItems,
  (value) => {
    localStorage.setItem(getStorageKey(PURCHASED_ITEMS_STORAGE_KEY), JSON.stringify(value));
  },
  { deep: true },
);

export function syncStorageForUser() {
  const cart = loadFromStorage(getStorageKey(CART_STORAGE_KEY), []);
  const ids = loadFromStorage(getStorageKey(PURCHASED_ID_STORAGE_KEY), []);
  const items = loadFromStorage(getStorageKey(PURCHASED_ITEMS_STORAGE_KEY), []);
  globalState.cart = cart;
  globalState.confirmed = new Set(ids);
  globalState.purchasedItems = items;
}

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
    existingItem.recommendedCount = product.recommendedCount || 1;
  } else {
    globalState.cart.push({
      productCode: product.externalId,
      name: product.title || product.name,
      ingredientName: product.ingredientName,
      price: product.price,
      imageUrl: product.imageUrl,
      productUrl: product.productUrl,
      quantity: 1,
      recommendedCount: product.recommendedCount || 1,
      packageGram: product.packageGram || 0
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

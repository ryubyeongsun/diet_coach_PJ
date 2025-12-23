import { reactive, watch } from "vue";

const CART_STORAGE_KEY = "nncoach_cart";
const PURCHASED_STORAGE_KEY = "nncoach_purchased"; // New key for confirmed history

// Load initial cart from localStorage
const savedCart = localStorage.getItem(CART_STORAGE_KEY);
const initialCart = savedCart ? JSON.parse(savedCart) : [];

// Load initial purchased items
const savedPurchased = localStorage.getItem(PURCHASED_STORAGE_KEY);
const initialPurchased = savedPurchased ? JSON.parse(savedPurchased) : [];

export const globalState = reactive({
  isLoading: false,
  error: null,
  cart: initialCart,
  confirmed: new Set(initialPurchased), // Track confirmed items
  isWeightModalOpen: false,
});

// Sync cart to localStorage
watch(
  () => globalState.cart,
  (newCart) => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(newCart));
  },
  { deep: true }
);

// Sync confirmed to localStorage (Convert Set to Array for JSON)
watch(
  () => globalState.confirmed,
  (newConfirmed) => {
    localStorage.setItem(PURCHASED_STORAGE_KEY, JSON.stringify([...newConfirmed]));
  },
  { deep: true }
);

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
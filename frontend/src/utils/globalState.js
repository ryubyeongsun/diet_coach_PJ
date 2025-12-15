import { reactive } from 'vue';

export const globalState = reactive({
  isLoading: false,
  error: null,
});

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

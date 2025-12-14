<template>
  <div class="login-container">
    <NnCard class="login-card">
      <h2>로그인</h2>
      <form @submit.prevent="handleLogin">
        <NnInput
          v-model="email"
          type="email"
          placeholder="이메일"
          required
        />
        <NnInput
          v-model="password"
          type="password"
          placeholder="비밀번호"
          required
        />
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <NnButton type="submit" :disabled="isLoading">
          {{ isLoading ? '로그인 중...' : '로그인하기' }}
        </NnButton>
      </form>
      <div class="signup-link">
        <span>계정이 없으신가요?</span>
        <router-link to="/signup">회원가입</router-link>
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '@/api/authApi.js';
import { saveAuth } from '@/utils/auth.js';
import NnCard from '@/components/common/NnCard.vue';
import NnInput from '@/components/common/NnInput.vue';
import NnButton from '@/components/common/NnButton.vue';

const router = useRouter();

const email = ref('');
const password = ref('');
const errorMessage = ref('');
const isLoading = ref(false);

async function handleLogin() {
  if (isLoading.value) return;
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const response = await login({
      email: email.value,
      password: password.value,
    });

    if (response.success && response.data) {
      saveAuth(response.data.token, response.data.user);
      await router.push('/meal-plans');
    } else {
      errorMessage.value = response.message || '이메일 또는 비밀번호를 확인해 주세요.';
    }
  } catch (error) {
    console.error('Login failed:', error);
    errorMessage.value = '이메일 또는 비밀번호를 확인해 주세요.';
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f3f4f6;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 2rem;
}

h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
}

form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.error-message {
  color: #ef4444;
  font-size: 0.875rem;
  text-align: center;
  margin-top: 0.5rem;
}

.signup-link {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.875rem;
  color: #666;
}

.signup-link a {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 500;
}

.signup-link a:hover {
  text-decoration: underline;
}
</style>

<template>
  <div class="login-page">
    <div class="login-container">
      <NnCard class="login-card">
        <div class="login-header">
          <h2>남남코치에 오신 것을 환영합니다.</h2>
          <p>식단, 체중, 예산 관리를 한 번에 시작하세요.</p>
        </div>
        <h1>로그인</h1>
        <form @submit.prevent="handleLogin" class="login-form">
          <NnInput v-model="email" type="email" placeholder="이메일" required />
          <NnInput
            v-model="password"
            type="password"
            placeholder="비밀번호"
            required
          />
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          <NnButton type="submit" :disabled="isLoading">
            {{ isLoading ? "로그인 중..." : "로그인하기" }}
          </NnButton>
        </form>
        <div class="signup-link">
          <span>계정이 없으신가요?</span>
          <router-link to="/signup">회원가입</router-link>
        </div>
      </NnCard>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { login, fetchMe } from "@/api/authApi.js";
import { saveAuth } from "@/utils/auth.js";
import NnCard from "@/components/common/NnCard.vue";
import NnInput from "@/components/common/NnInput.vue";
import NnButton from "@/components/common/NnButton.vue";

const router = useRouter();

const email = ref("");
const password = ref("");
const errorMessage = ref("");
const isLoading = ref(false);

async function handleLogin() {
  if (isLoading.value) return;
  isLoading.value = true;
  errorMessage.value = "";

  try {
    const loginResponse = await login({
      email: email.value,
      password: password.value,
    });

    if (loginResponse.data && loginResponse.data.accessToken) {
      const token = loginResponse.data.accessToken;
      // 1. 토큰만 우선 저장 (다음 API 호출을 위해)
      saveAuth(token, null);

      // 2. 사용자 정보 가져오기
      const meResponse = await fetchMe();
      const user = meResponse.data;

      // 3. 토큰과 사용자 정보 함께 저장
      saveAuth(token, user);

      await router.push("/meal-plans");
    } else {
      errorMessage.value =
        loginResponse.message || "이메일 또는 비밀번호를 확인해 주세요.";
    }
  } catch (error) {
    console.error("Login failed:", error);
    errorMessage.value = "이메일 또는 비밀번호를 확인해 주세요.";
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: #f3f4f6;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 16px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 8px;
}

.login-header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

h1 {
  text-align: center;
  margin-bottom: 24px;
  font-size: 24px;
  color: #1f2937;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-message {
  color: #dc2626;
  font-size: 14px;
  text-align: center;
}

.signup-link {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.signup-link a {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.signup-link a:hover {
  text-decoration: underline;
}
</style>

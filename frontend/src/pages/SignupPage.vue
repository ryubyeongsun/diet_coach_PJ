<template>
  <div class="signup-container">
    <NnCard class="signup-card">
      <h2>회원가입</h2>
      <form @submit.prevent="handleSignup">
        <NnInput v-model="name" type="text" placeholder="이름" required />
        <NnInput v-model="email" type="email" placeholder="이메일" required />
        <NnInput
          v-model="password"
          type="password"
          placeholder="비밀번호"
          required
        />
        <NnInput
          v-model="passwordConfirm"
          type="password"
          placeholder="비밀번호 확인"
          required
        />
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <NnButton type="submit" :disabled="isLoading">
          {{ isLoading ? "가입 처리 중..." : "회원가입" }}
        </NnButton>
      </form>
      <div class="login-link">
        <span>이미 계정이 있으신가요?</span>
        <router-link to="/login">로그인</router-link>
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { useRouter } from "vue-router";
import { signup } from "@/api/authApi.js";
import NnCard from "@/components/common/NnCard.vue";
import NnInput from "@/components/common/NnInput.vue";
import NnButton from "@/components/common/NnButton.vue";

const router = useRouter();

const name = ref("");
const email = ref("");
const password = ref("");
const passwordConfirm = ref("");
const errorMessage = ref("");
const isLoading = ref(false);

watch([password, passwordConfirm], () => {
  if (password.value !== passwordConfirm.value && passwordConfirm.value) {
    errorMessage.value = "비밀번호가 일치하지 않습니다.";
  } else {
    errorMessage.value = "";
  }
});

async function handleSignup() {
  if (password.value !== passwordConfirm.value) {
    errorMessage.value = "비밀번호가 일치하지 않습니다.";
    return;
  }
  if (!name.value.trim() || !email.value.trim() || !password.value.trim()) {
    errorMessage.value = "모든 필드를 입력해 주세요.";
    return;
  }
  if (isLoading.value) return;

  isLoading.value = true;
  errorMessage.value = "";

  try {
    const response = await signup({
      name: name.value,
      email: email.value,
      password: password.value,
    });

    if (response.success) {
      alert("회원가입이 완료되었습니다. 로그인해 주세요.");
      await router.push("/login");
    } else {
      errorMessage.value =
        response.message ||
        "회원가입에 실패했습니다. 입력 정보를 확인해 주세요.";
    }
  } catch (error) {
    console.error("Signup failed:", error);
    errorMessage.value =
      error.response?.data?.message || "이미 사용 중인 이메일입니다.";
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.signup-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f3f4f6;
}

.signup-card {
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

.login-link {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.875rem;
  color: #666;
}

.login-link a {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>

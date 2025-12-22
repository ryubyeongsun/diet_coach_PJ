<template>
  <div class="signup-page">
    <div class="signup-container">
      <div class="signup-card">
        <!-- ✨ 가상 애니메이션 캐릭터 (SVG) -->
        <div class="mascot-container">
          <svg class="mascot-svg" viewBox="0 0 200 120" xmlns="http://www.w3.org/2000/svg">
            <path d="M40 120 C 40 50, 160 50, 160 120" fill="#86efac" />
            <circle cx="70" cy="90" r="5" fill="#fca5a5" opacity="0.6" />
            <circle cx="130" cy="90" r="5" fill="#fca5a5" opacity="0.6" />
            <g class="eyes">
              <circle cx="80" cy="80" r="6" fill="#1f2937" />
              <circle cx="120" cy="80" r="6" fill="#1f2937" />
            </g>
            <g class="hands">
              <circle cx="50" cy="115" r="12" fill="#4ade80" stroke="#166534" stroke-width="2" />
              <circle cx="150" cy="115" r="12" fill="#4ade80" stroke="#166534" stroke-width="2" />
            </g>
          </svg>
        </div>

        <div class="brand">
          <span class="logo-icon">🥑</span> 남남코치
        </div>

        <div class="header">
          <h1>회원가입</h1>
          <p>3초만에 가입하고 시작하세요!</p>
        </div>

        <form @submit.prevent="handleSignup" class="auth-form">
          <div class="input-group">
            <NnInput v-model="name" type="text" placeholder="이름" required />
          </div>
          <div class="input-group">
            <NnInput v-model="email" type="email" placeholder="이메일" required />
          </div>
          <div class="input-group">
            <NnInput
              v-model="password"
              type="password"
              placeholder="비밀번호"
              required
            />
          </div>
          <div class="input-group">
            <NnInput
              v-model="passwordConfirm"
              type="password"
              placeholder="비밀번호 확인"
              required
            />
          </div>

          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

          <NnButton type="submit" :disabled="isLoading" class="submit-btn">
            {{ isLoading ? "가입 중..." : "회원가입" }}
          </NnButton>
        </form>

        <div class="footer-link">
          이미 계정이 있으신가요? <router-link to="/login">로그인</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { useRouter } from "vue-router";
import { signup, login, fetchMe } from "@/api/authApi.js";
import { saveAuth } from "@/utils/auth.js";
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
      // 자동 로그인
      const loginResponse = await login({
        email: email.value,
        password: password.value,
      });

      if (loginResponse.data && loginResponse.data.accessToken) {
        const token = loginResponse.data.accessToken;
        saveAuth(token, null);
        const meResponse = await fetchMe();
        const user = meResponse.data;
        saveAuth(token, user);
        await router.push("/profile/setup");
      } else {
        alert("회원가입은 완료되었으나 자동 로그인에 실패했습니다.");
        await router.push("/login");
      }
    } else {
      errorMessage.value = response.message || "회원가입 실패";
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
.signup-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  background-color: #f0fdf4;
  background-image: url('/images/character.png.png');
  background-size: contain;
  background-repeat: repeat;
  background-position: center;
}

.signup-page::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
  z-index: 0;
}

.signup-container {
  width: 100%;
  max-width: 420px;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.signup-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 50px 32px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.8);
  position: relative;
  margin-top: 40px;
}

.mascot-container {
  position: absolute;
  top: -95px;
  left: 50%;
  transform: translateX(-50%);
  width: 160px;
  height: 100px;
  z-index: 2;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1));
  animation: float 3s ease-in-out infinite;
}

.mascot-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.eyes {
  animation: blink 4s infinite;
  transform-origin: center;
}

@keyframes float {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, -6px); }
}

@keyframes blink {
  0%, 48%, 52%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.1); }
}

.brand {
  font-size: 18px;
  font-weight: 800;
  color: #047857;
  text-align: center;
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 28px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
}

.header p {
  color: #4b5563;
  font-size: 15px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.submit-btn {
  height: 50px;
  font-size: 16px;
  margin-top: 12px;
  border-radius: 12px;
  background: #047857;
  border: none;
}
.submit-btn:hover {
  background: #065f46;
}

.error-message {
  color: #ef4444;
  font-size: 14px;
  text-align: center;
  font-weight: 500;
}

.footer-link {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #4b5563;
}
.footer-link a {
  color: #047857;
  font-weight: 700;
  text-decoration: none;
}
.footer-link a:hover {
  text-decoration: underline;
}
</style>
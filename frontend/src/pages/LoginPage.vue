<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <!-- ✨ 가상 애니메이션 캐릭터 (SVG) -->
        <div class="mascot-container">
          <svg class="mascot-svg" viewBox="0 0 200 120" xmlns="http://www.w3.org/2000/svg">
            <!-- 몸통 (빼꼼) -->
            <path d="M40 120 C 40 50, 160 50, 160 120" fill="#86efac" />
            <!-- 얼굴 (아보카도 씨앗 느낌의 볼터치) -->
            <circle cx="70" cy="90" r="5" fill="#fca5a5" opacity="0.6" />
            <circle cx="130" cy="90" r="5" fill="#fca5a5" opacity="0.6" />
            
            <!-- 눈 (깜빡임 애니메이션) -->
            <g class="eyes">
              <circle cx="80" cy="80" r="6" fill="#1f2937" />
              <circle cx="120" cy="80" r="6" fill="#1f2937" />
            </g>
            
            <!-- 손 (카드를 잡고 있는 모양) -->
            <g class="hands">
              <circle cx="50" cy="115" r="12" fill="#4ade80" stroke="#166534" stroke-width="2" />
              <circle cx="150" cy="115" r="12" fill="#4ade80" stroke="#166534" stroke-width="2" />
            </g>
          </svg>
        </div>

        <div class="brand">
          <img src="/images/brand-logo.png" alt="남남코치" class="logo-img" />
        </div>
        
        <div class="header">
          <h1>로그인</h1>
          <p>오늘도 건강한 하루 되세요!</p>
        </div>

        <form @submit.prevent="handleLogin" class="auth-form">
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
          
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          
          <NnButton type="submit" :disabled="isLoading" class="submit-btn">
            {{ isLoading ? "로그인 중..." : "로그인" }}
          </NnButton>
        </form>

        <div class="footer-link">
          계정이 없으신가요? <router-link to="/signup">회원가입</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { login, fetchMe } from "@/api/authApi.js";
import { saveAuth } from "@/utils/auth.js";
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
      saveAuth(token, null);
      const meResponse = await fetchMe();
      const user = meResponse.data;
      saveAuth(token, user);
      await router.push("/dashboard");
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
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  /* 배경 이미지 개선: 비율 유지하며 반복 or 꽉 채우기 */
  background-color: #f0fdf4;
  background-image: url('/images/character.png.png');
  background-size: contain; /* 짤림 방지 */
  background-repeat: repeat; /* 패턴처럼 반복 (또는 no-repeat & position center) */
  background-position: center;
}

.login-page::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.4); /* 밝은 오버레이로 변경 */
  backdrop-filter: blur(8px);
  z-index: 0;
}

.login-container {
  width: 100%;
  max-width: 420px;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.login-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 50px 32px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.8);
  position: relative;
  margin-top: 40px; /* 캐릭터 공간 확보 */
}

/* --- SVG 마스코트 스타일 --- */
.mascot-container {
  position: absolute;
  top: -95px; /* 카드 위로 배치 */
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

/* 눈 깜빡임 애니메이션 */
.eyes {
  animation: blink 4s infinite;
  transform-origin: center;
}

/* 둥둥 떠다니는 애니메이션 */
@keyframes float {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, -6px); }
}

@keyframes blink {
  0%, 48%, 52%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.1); }
}

.brand {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.logo-img {
  height: 48px;
  width: auto;
  object-fit: contain;
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

.input-group {
  display: flex;
  flex-direction: column;
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

import { createRouter, createWebHistory } from "vue-router";
import LandingPage from "../pages/LandingPage.vue";
import DashboardPage from "../pages/DashboardPage.vue";
import MealPlanPage from "../pages/MealPlanPage.vue";
import ShoppingPage from "../pages/ShoppingPage.vue";
import WeightPage from "../pages/WeightPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import SignupPage from "../pages/SignupPage.vue";
import ProfileSetupPage from "../pages/ProfileSetupPage.vue";
import CartPage from "../pages/CartPage.vue";
import { getCurrentUser, getToken, clearAuth } from "../utils/auth"; // getToken, clearAuth 추가 임포트

const routes = [
  {
    path: "/",
    name: "Landing",
    component: LandingPage,
  },
  {
    path: "/login",
    name: "Login",
    component: LoginPage,
  },
  {
    path: "/signup",
    name: "Signup",
    component: SignupPage,
  },
  {
    path: "/profile/setup",
    name: "ProfileSetup",
    component: ProfileSetupPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/cart",
    name: "Cart",
    component: CartPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/dashboard",
    name: "Dashboard",
    component: DashboardPage,
    meta: { requiresAuth: true, requiresProfile: true },
  },
  {
    path: "/meal-plans",
    name: "meal-plans",
    component: MealPlanPage,
    meta: { requiresAuth: true, requiresProfile: true },
  },
  {
    path: "/shopping",
    name: "shopping",
    component: ShoppingPage,
    meta: { requiresAuth: true, requiresProfile: true },
  },
  {
    path: "/weights",
    name: "weights",
    component: WeightPage,
    meta: { requiresAuth: true, requiresProfile: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 3. 네비게이션 가드 수정
router.beforeEach((to, from, next) => {
  const token = getToken(); // 토큰 존재 여부를 직접 확인
  const user = getCurrentUser(); // 유저 정보는 프로필 체크를 위해 유지
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
  const requiresProfile = to.matched.some(
    (record) => record.meta.requiresProfile,
  );

  // 인증이 필요한 페이지인데 토큰이 없는 경우 -> 로그인 페이지로
  if (requiresAuth && !token) {
    const wasLoggedIn = !!user; // 이전에 로그인 기록이 있었는지 확인
    if (wasLoggedIn) {
      clearAuth(); // 남아있는 user 정보 등을 정리
      alert("로그인 시간이 만료되었습니다.\n다시 로그인해 주세요.");
    }
    return next({ path: "/login" });
  }

  // 로그인/회원가입 페이지에 접속했는데 이미 토큰이 있는 경우 -> 대시보드 페이지로
  if ((to.path === "/login" || to.path === "/signup") && token) {
    return next({ path: "/dashboard" });
  }

  // 프로필이 필요한 페이지에 접속했는데 프로필 정보가 없는 경우 -> 프로필 설정 페이지로
  // (단, 토큰이 있어야 프로필 체크를 진행)
  if (requiresProfile && token && user) {
    // 토큰도 있고 유저 객체도 있을 때만 프로필 체크
    const isProfileComplete =
      user.gender &&
      user.birthDate &&
      user.height &&
      user.weight &&
      user.activityLevel &&
      user.goalType;
    if (!isProfileComplete) {
      // 무한 리다이렉션 방지
      if (to.path !== "/profile/setup") {
        return next({ path: "/profile/setup" });
      }
    }
  }

  // 모든 조건 통과
  next();
});

export default router;

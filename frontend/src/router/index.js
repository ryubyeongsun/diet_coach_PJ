import { createRouter, createWebHistory } from 'vue-router';
import MealPlanPage from '../pages/MealPlanPage.vue';
import ShoppingPage from '../pages/ShoppingPage.vue';
import WeightPage from '../pages/WeightPage.vue';
import LoginPage from '../pages/LoginPage.vue';
import SignupPage from '../pages/SignupPage.vue';
import ProfileSetupPage from '../pages/ProfileSetupPage.vue';
import CartPage from '../pages/CartPage.vue';
import { getCurrentUser } from '../utils/auth';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
  },
  {
    path: '/signup',
    name: 'Signup',
    component: SignupPage,
  },
  {
    path: '/profile/setup',
    name: 'ProfileSetup',
    component: ProfileSetupPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/cart',
    name: 'Cart',
    component: CartPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/',
    component: MealPlanPage, // 임시로 홈 = 식단 페이지
    meta: { requiresAuth: true, requiresProfile: true },
  },
  {
    path: '/meal-plans',
    name: 'meal-plans',
    component: MealPlanPage,
    meta: { requiresAuth: true, requiresProfile: true },
  },
  {
    path: '/shopping',
    name: 'shopping',
    component: ShoppingPage,
    meta: { requiresAuth: true, requiresProfile: true },
  },
  {
    path: '/weights',
    name: 'weights',
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
  const user = getCurrentUser();
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const requiresProfile = to.matched.some(record => record.meta.requiresProfile);

  // 인증이 필요한 페이지인데 유저가 없는 경우 -> 로그인 페이지로
  if (requiresAuth && !user) {
    return next({ path: '/login' });
  }

  // 로그인/회원가입 페이지에 접속했는데 이미 유저가 있는 경우 -> 메인 페이지로
  if ((to.path === '/login' || to.path === '/signup') && user) {
    return next({ path: '/meal-plans' });
  }

  // 프로필이 필요한 페이지에 접속했는데 프로필 정보가 없는 경우 -> 프로필 설정 페이지로
  if (requiresProfile && user) {
    const isProfileComplete = user.gender && user.birthDate && user.height && user.weight && user.activityLevel && user.goalType;
    if (!isProfileComplete) {
      // 무한 리다이렉션 방지
      if (to.path !== '/profile/setup') {
        return next({ path: '/profile/setup' });
      }
    }
  }
  
  // 모든 조건 통과
  next();
});

export default router;

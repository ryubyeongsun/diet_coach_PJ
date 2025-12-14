import { createRouter, createWebHistory } from 'vue-router';
import MealPlanPage from '../pages/MealPlanPage.vue';
import ShoppingPage from '../pages/ShoppingPage.vue';
import WeightPage from '../pages/WeightPage.vue';
import LoginPage from '../pages/LoginPage.vue';
import SignupPage from '../pages/SignupPage.vue';
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
    path: '/',
    component: MealPlanPage, // 임시로 홈 = 식단 페이지
    meta: { requiresAuth: true },
  },
  {
    path: '/meal-plans',
    name: 'meal-plans',
    component: MealPlanPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/shopping',
    name: 'shopping',
    component: ShoppingPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/weights',
    name: 'weights',
    component: WeightPage,
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const user = getCurrentUser();

  if (requiresAuth && !user) {
    next({ path: '/login' });
  } else if ((to.path === '/login' || to.path === '/signup') && user) {
    next({ path: '/meal-plans' });
  } else {
    next();
  }
});

export default router;

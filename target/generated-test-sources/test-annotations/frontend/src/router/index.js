import { createRouter, createWebHistory } from 'vue-router';
import MealPlanPage from '../pages/MealPlanPage.vue';
import ShoppingPage from '../pages/ShoppingPage.vue';

const routes = [
  {
    path: '/',
    component: MealPlanPage, // 임시로 홈 = 식단 페이지
  },
  {
    path: '/meal-plans',
    component: MealPlanPage,
  },
  {
    path: '/shopping',
    component: ShoppingPage,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;

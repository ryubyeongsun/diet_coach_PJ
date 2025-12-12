import { createRouter, createWebHistory } from 'vue-router';
import MealPlanPage from '../pages/MealPlanPage.vue';
import ShoppingPage from '../pages/ShoppingPage.vue';
import WeightPage from '../pages/WeightPage.vue';

const routes = [
  {
    path: '/',
    component: MealPlanPage, // 임시로 홈 = 식단 페이지
  },
  {
    path: '/meal-plans',
    name: 'meal-plans',
    component: MealPlanPage,
  },
  {
    path: '/shopping',
    name: 'shopping',
    component: ShoppingPage,
  },
  {
    path: '/weights',
    name: 'weights',
    component: WeightPage,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;

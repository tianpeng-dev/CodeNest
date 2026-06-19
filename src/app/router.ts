import { createRouter, createWebHistory } from 'vue-router';

const AppBootstrapPage = {
  template: '<main class="page-shell"></main>',
};

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppBootstrapPage,
    },
  ],
});

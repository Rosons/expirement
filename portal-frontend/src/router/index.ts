import { createRouter, createWebHistory } from 'vue-router';
import ChatProductView from '../views/ChatProductView.vue';
import PortalHomeView from '../views/PortalHomeView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'portal-home',
      component: PortalHomeView,
    },
    {
      path: '/products/chat',
      name: 'product-chat',
      component: ChatProductView,
    },
  ],
});

export default router;

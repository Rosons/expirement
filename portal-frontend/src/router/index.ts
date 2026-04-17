import { createRouter, createWebHistory } from 'vue-router';
import PortalHomeView from '../views/portal-home/index.vue';

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
      component: () => import('../views/general-chat/index.vue'),
    },
    {
      path: '/products/knowledge-chat',
      name: 'product-knowledge-chat',
      component: () => import('../views/knowledge-chat/index.vue'),
    },
    {
      path: '/products/game-chat',
      name: 'product-game-chat',
      component: () => import('../views/game-chat-entry/index.vue'),
    },
    {
      path: '/products/game-chat/play',
      name: 'product-game-chat-play',
      component: () => import('../views/game-chat/index.vue'),
    },
    {
      path: '/products/customer-chat',
      name: 'product-customer-chat',
      component: () => import('../views/customer-chat/index.vue'),
    },
  ],
});

export default router;

import { createRouter, createWebHistory } from 'vue-router';
import GeneralChatView from '../views/GeneralChatView.vue';
import GameChatView from '../views/GameChatView.vue';
import GameChatEntryView from '../views/GameChatEntryView.vue';
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
      component: GeneralChatView,
    },
    {
      path: '/products/game-chat',
      name: 'product-game-chat',
      component: GameChatEntryView,
    },
    {
      path: '/products/game-chat/play',
      name: 'product-game-chat-play',
      component: GameChatView,
    },
  ],
});

export default router;

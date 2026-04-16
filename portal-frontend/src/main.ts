import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import 'highlight.js/styles/atom-one-dark.css';
import './style.css';

createApp(App).use(router).mount('#app');

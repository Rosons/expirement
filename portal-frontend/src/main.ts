import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import 'highlight.js/styles/atom-one-dark.css';
import './style.css';

createApp(App).use(router).mount('#app');

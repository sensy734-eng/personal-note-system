import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';

import App from './App.vue';
import Login from './views/Login.vue';
import Home from './views/Home.vue';
import EditNote from './views/EditNote.vue';

const requireAuth = (to, from, next) => {
    const token = localStorage.getItem('token');
    if (!token) next('/login');
    else next();
};

const routes = [
    { path: '/login', component: Login },
    { path: '/', component: Home, beforeEnter: requireAuth },
    { path: '/note/new', component: EditNote, beforeEnter: requireAuth },
    { path: '/note/edit/:id', component: EditNote, beforeEnter: requireAuth, props: true }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

const app = createApp(App);
app.use(router);
app.use(ElementPlus);
app.mount('#app');

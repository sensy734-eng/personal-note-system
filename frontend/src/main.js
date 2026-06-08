import { createApp } from 'vue'
import App from './App.vue'
import { createRouter, createWebHistory } from 'vue-router'

// 💡 引入 Element Plus 及核心样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 🚀 新增：引入暗黑模式变量样式（必需）
import 'element-plus/theme-chalk/dark/css-vars.css'

import Login from './views/Login.vue'
import Home from './views/Home.vue'
import EditNote from './views/EditNote.vue'

// 路由守卫：未登录跳转到登录页
const requireAuth = (to, from, next) => {
    const token = localStorage.getItem('token')
    if (!token) next('/login')
    else next()
}

const routes = [
    { path: '/login', component: Login },
    { path: '/', component: Home, beforeEnter: requireAuth },
    { path: '/note/new', component: EditNote, beforeEnter: requireAuth },
    { path: '/note/edit/:id', component: EditNote, beforeEnter: requireAuth, props: true },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

const app = createApp(App)

// 💡 注册路由和组件库，并挂载到 #app 节点
app.use(router)
app.use(ElementPlus)
app.mount('#app')
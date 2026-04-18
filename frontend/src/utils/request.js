import axios from 'axios';
import { ElMessage } from 'element-plus'; // 引入 Element Plus 的消息提示

// 创建 axios 实例
const service = axios.create({
    // 优化：生产环境使用云端 API 地址，开发环境使用本地代理
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api', 
    timeout: 5000
});

// 🚀 请求拦截器：负责在每次请求前“塞入” Token
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 🚀 响应拦截器：负责在收到响应后进行“安检”
service.interceptors.response.use(
    response => {
        // 如果 HTTP 状态码是 2xx，说明请求成功，直接剥离并返回核心数据
        return response.data;
    },
    error => {
        // 当发生错误时（如 401、403、500 等）
        if (error.response) {
            const status = error.response.status;

            if (status === 401) {
                // 💡 核心保护机制：拦截 401 身份过期
                console.warn('检测到身份过期，正在拦截并跳转...');
                
                // 1. 清空失效的本地缓存
                localStorage.removeItem('token');
                localStorage.removeItem('userInfo');
                
                // 2. 友好提示用户
                ElMessage.error('登录状态已过期，请重新登录');
                
                // 3. 强制跳转回登录页 (使用原生 location 避免路由循环依赖)
                // 延迟 500ms 跳转，让用户看清错误提示
                setTimeout(() => {
                    window.location.href = '/login';
                }, 500);

            } else {
                // 拦截其他类型的后端报错 (如 400 密码错误, 404 找不到笔记)
                // 优先展示后端返回的具体错误 message，如果没有则显示默认提示
                const errorMsg = error.response.data?.message || '请求失败，请稍后重试';
                ElMessage.error(errorMsg);
            }
        } else {
            // 处理服务器彻底宕机或断网的情况
            ElMessage.error('网络连接异常，无法连接到服务器');
        }
        
        return Promise.reject(error);
    }
);

export default service;

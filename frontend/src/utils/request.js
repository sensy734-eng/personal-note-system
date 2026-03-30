import axios from 'axios';

// 创建 axios 实例
const service = axios.create({
    baseURL: 'http://localhost:8080/api', // 💡 修复了这里的冗余字符和引号错误
    timeout: 5000
});

// 请求拦截器 (后续在这里加上 JWT Token)
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

// 响应拦截器
service.interceptors.response.use(
    response => response.data,
    error => {
        console.error('请求错误:', error);
        return Promise.reject(error);
    }
);

export default service;
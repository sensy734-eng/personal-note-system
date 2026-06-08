import axios from 'axios';
import { ElMessage } from 'element-plus';

export const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const buildApiUrl = (path) => {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    if (apiBaseURL === '/api') return `/api${normalizedPath}`;
    return `${apiBaseURL.replace(/\/$/, '')}${normalizedPath}`;
};

const service = axios.create({
    baseURL: apiBaseURL,
    timeout: 60000
});

service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

service.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response) {
            if (error.response.status === 401) {
                localStorage.removeItem('token');
                localStorage.removeItem('userInfo');
                ElMessage.error('登录状态已过期，请重新登录');
                setTimeout(() => {
                    window.location.href = '/login';
                }, 500);
            } else {
                ElMessage.error(error.response.data?.message || '请求失败，请稍后重试');
            }
        } else {
            ElMessage.error('无法连接到服务器，请确认后端服务已启动');
        }
        return Promise.reject(error);
    }
);

export default service;

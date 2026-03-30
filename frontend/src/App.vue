<template>
  <router-view></router-view>
</template>

<script setup>
import { onMounted } from 'vue';

onMounted(() => {
  // 🚀 初始化：检查本地存储的主题偏好
  const savedTheme = localStorage.getItem('theme-mode');
  if (savedTheme === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
});
</script>

<style>
/* 💡 全局样式重置 */
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
  'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  /* 增加平滑过渡效果 */
  transition: background-color 0.3s, color 0.3s;
}

#app {
  height: 100vh;
}

/* 适配暗黑模式下的 html 背景色，防止切换瞬间闪白 */
html.dark {
  background-color: #121212;
  color-scheme: dark;
}
</style>
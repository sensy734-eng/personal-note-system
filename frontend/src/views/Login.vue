<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <h2 class="title">{{ modeTitle }} - 个人学习笔记</h2>

      <el-form :model="form" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="mode !== 'login'" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" clearable>
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="mode !== 'reset'" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="mode === 'reset'" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码" show-password clearable>
            <template #prefix><el-icon><CircleCheck /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleSubmit" :loading="loading">
            {{ modeBtnText }}
          </el-button>
        </el-form-item>

        <div class="toggle-container">
          <span class="toggle-text" @click="switchMode(mode === 'login' ? 'register' : 'login')">
            {{ mode === 'login' ? '没有账号？点击注册' : '已有账号？返回登录' }}
          </span>
          <span v-if="mode === 'login'" class="forget-pwd" @click="switchMode('reset')">
            忘记密码？
          </span>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { User, Lock, Message, CircleCheck } from '@element-plus/icons-vue';
import request from '../utils/request';

const router = useRouter();
const mode = ref(window.location.hash === '#register' ? 'register' : 'login');
const loading = ref(false);
const formRef = ref(null);

const form = reactive({
  username: '',
  password: '',
  email: '',
  newPassword: ''
});

const modeTitle = computed(() => ({ login: '登录', register: '注册', reset: '重置密码' }[mode.value]));
const modeBtnText = computed(() => ({ login: '登录', register: '注册', reset: '确认重置' }[mode.value]));

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码至少 6 位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码至少 6 位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
};

const switchMode = (targetMode) => {
  mode.value = targetMode;
  window.location.hash = targetMode === 'register' ? 'register' : (targetMode === 'reset' ? 'reset' : '');
  if (formRef.value) formRef.value.resetFields();
};

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;

    loading.value = true;
    try {
      let url = '';
      let data = {};

      if (mode.value === 'login') {
        url = '/auth/login';
        data = { username: form.username, password: form.password };
      } else if (mode.value === 'register') {
        url = '/auth/register';
        data = { username: form.username, password: form.password, email: form.email };
      } else {
        url = '/auth/reset-password';
        data = { username: form.username, email: form.email, newPassword: form.newPassword };
      }

      const res = await request.post(url, data);
      ElMessage.success(res.message || '操作成功');

      if (mode.value === 'login') {
        localStorage.setItem('token', res.token);
        localStorage.setItem('userInfo', JSON.stringify(res.user));
        router.push('/');
      } else {
        switchMode('login');
        form.password = '';
        form.newPassword = '';
      }
    } finally {
      loading.value = false;
    }
  });
};

onMounted(() => {
  window.addEventListener('hashchange', () => {
    const currentHash = window.location.hash;
    if (currentHash === '#register') mode.value = 'register';
    else if (currentHash === '#reset') mode.value = 'reset';
    else mode.value = 'login';
  });
});
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.login-card {
  width: 420px;
  border-radius: 10px;
}

.title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.submit-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 2px;
}

.toggle-container {
  margin-top: 15px;
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.toggle-text,
.forget-pwd {
  color: #409eff;
  cursor: pointer;
  user-select: none;
  transition: color 0.3s;
}

.toggle-text:hover,
.forget-pwd:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.forget-pwd {
  color: #909399;
}
</style>

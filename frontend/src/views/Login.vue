<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const message = ref('')
const authStore = useAuthStore()
const router = useRouter()

const handleLogin = async () => {
  try {
    await authStore.login(username.value, password.value)
    router.push('/')
  } catch (error) {
    message.value = '登录失败: ' + (error.response?.data?.message || error.message)
  }
}
</script>

<template>
  <div class="row justify-content-center align-items-center min-vh-75">
    <div class="col-md-5">
      <div class="glass-panel p-4 p-md-5" style="border-radius: 28px;">
        <div class="text-center mb-5">
          <h1 class="h2 fw-bold mb-2">欢迎回来</h1>
          <p class="text-secondary">请输入您的账号密码以继续</p>
        </div>
        
        <form @submit.prevent="handleLogin">
          <div class="mb-4">
            <label for="username" class="form-label">用户名</label>
            <input 
              type="text" 
              class="form-control" 
              id="username" 
              v-model="username" 
              placeholder="请输入用户名"
              required
            >
          </div>
          <div class="mb-5">
            <label for="password" class="form-label">密码</label>
            <input 
              type="password" 
              class="form-control" 
              id="password" 
              v-model="password" 
              placeholder="请输入密码"
              required
            >
          </div>
          <button type="submit" class="btn btn-primary w-100 py-3 mb-4 shadow-sm">
            <span>登录</span>
          </button>
          
          <div v-if="message" class="alert alert-danger border-0 rounded-4 py-3 mb-4" style="background-color: rgba(255, 59, 48, 0.1); color: var(--apple-red);">
            <i class="bi bi-exclamation-circle me-2"></i>{{ message }}
          </div>
          
          <div class="text-center">
            <p class="text-secondary small mb-0">还没有账号？ 
              <RouterLink to="/register" class="text-primary fw-600 text-decoration-none">立即注册</RouterLink>
            </p>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.min-vh-75 {
  min-height: 75vh;
}

.fw-600 {
  font-weight: 600;
}
</style>

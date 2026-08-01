<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const email = ref('')
const message = ref('')
const successful = ref(false)
const authStore = useAuthStore()
const router = useRouter()

const handleRegister = async () => {
  message.value = ''
  successful.value = false
  try {
    await authStore.register({
      username: username.value,
      email: email.value,
      password: password.value
    })
    message.value = '注册成功!'
    successful.value = true
    setTimeout(() => router.push('/login'), 2000)
  } catch (error) {
    message.value = '注册失败: ' + (error.response?.data?.message || error.message)
    successful.value = false
  }
}
</script>

<template>
  <div class="row justify-content-center align-items-center min-vh-75">
    <div class="col-md-5">
      <div class="glass-panel p-4 p-md-5" style="border-radius: 28px;">
        <div class="text-center mb-5">
          <h1 class="h2 fw-bold mb-2">开启学习之旅</h1>
          <p class="text-secondary">创建一个账号，开始您的在线考试体验</p>
        </div>
        
        <form @submit.prevent="handleRegister">
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
          <div class="mb-4">
            <label for="email" class="form-label">邮箱</label>
            <input 
              type="email" 
              class="form-control" 
              id="email" 
              v-model="email" 
              placeholder="请输入邮箱地址"
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
            <span>注册</span>
          </button>
          
          <div v-if="message" 
               :class="successful ? 'alert alert-success border-0 rounded-4' : 'alert alert-danger border-0 rounded-4'" 
               class="py-3 mb-4"
               :style="successful ? 'background-color: rgba(52, 199, 89, 0.1); color: var(--apple-green);' : 'background-color: rgba(255, 59, 48, 0.1); color: var(--apple-red);'">
            <i :class="successful ? 'bi bi-check-circle me-2' : 'bi bi-exclamation-circle me-2'"></i>
            {{ message }}
          </div>
          
          <div class="text-center">
            <p class="text-secondary small mb-0">已有账号？ 
              <RouterLink to="/login" class="text-primary fw-600 text-decoration-none">返回登录</RouterLink>
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

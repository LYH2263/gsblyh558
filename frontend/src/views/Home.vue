<script setup>
import { useAuthStore } from '../stores/auth'
const authStore = useAuthStore()
</script>

<template>
  <div class="container">
    <div class="row justify-content-center align-items-center min-vh-75 py-2">
      <div class="col-lg-10 text-center">
        <div class="mb-5 fade-in">
          <h1 class="apple-title mb-3">在线考试系统</h1>
          <p class="lead text-secondary mb-5 fs-4 mx-auto" style="max-width: 700px;">
            专业的在线练习与考试平台，基于苹果设计语言，<br>为您提供最极致的学习体验。
          </p>
        </div>
        
        <div v-if="!authStore.user" class="d-flex justify-content-center gap-4 fade-in-up">
          <RouterLink to="/login" class="btn btn-primary btn-lg shadow-lg">
            立即登录
          </RouterLink>
          <RouterLink to="/register" class="btn btn-outline-primary btn-lg">
            注册账号
          </RouterLink>
        </div>
        
        <div v-else class="glass-panel p-5 mx-auto fade-in-up" style="max-width: 650px; border-radius: 24px;">
          <h2 class="h3 mb-2 fw-bold text-primary">欢迎回来, {{ authStore.user.username }}!</h2>
          <p class="text-secondary mb-5">准备好开始今天的学习了吗？</p>
          <div class="row g-4 justify-content-center">
            <div class="col-md-4">
              <RouterLink to="/practice" class="btn btn-primary w-100 py-3 rounded-4 shadow-sm d-flex align-items-baseline justify-content-center gap-2">
                <i class="bi bi-pencil-square fs-4"></i>
                <span class="fw-bold">开始练习</span>
              </RouterLink>
            </div>
            <div class="col-md-4">
              <RouterLink to="/exams" class="btn btn-outline-primary w-100 py-3 rounded-4 d-flex align-items-baseline justify-content-center gap-2">
                <i class="bi bi-journal-check fs-4"></i>
                <span class="fw-bold">参加考试</span>
              </RouterLink>
            </div>
            <div class="col-md-4">
              <RouterLink to="/wrong-questions" class="btn btn-outline-primary w-100 py-3 rounded-4 d-flex align-items-baseline justify-content-center gap-2">
                <i class="bi bi-journal-x fs-4"></i>
                <span class="fw-bold">错题本</span>
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.min-vh-75 {
  min-height: 75vh;
}

.fade-in {
  animation: fadeIn 0.8s ease-out forwards;
}

.fade-in-up {
  animation: fadeInUp 0.8s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
  opacity: 0;
  transform: translateY(30px);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

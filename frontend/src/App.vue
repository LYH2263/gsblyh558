<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useRouter } from 'vue-router'
import ToastContainer from './components/ToastContainer.vue'

const authStore = useAuthStore()
const router = useRouter()

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <ToastContainer />
  <header>
    <nav class="navbar navbar-expand-lg glass-nav mb-1 sticky-top">
      <div class="container d-flex align-items-center">
        <RouterLink class="navbar-brand d-flex align-items-center" to="/">在线考试系统</RouterLink>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav me-auto align-items-center">
            <li class="nav-item">
              <RouterLink class="nav-link" to="/">首页</RouterLink>
            </li>
            <li class="nav-item" v-if="authStore.user">
              <RouterLink class="nav-link" to="/practice">练习</RouterLink>
            </li>
            <li class="nav-item" v-if="authStore.user">
              <RouterLink class="nav-link" to="/exams">考试</RouterLink>
            </li>
            <li class="nav-item" v-if="authStore.user">
              <RouterLink class="nav-link" to="/wrong-questions">错题本</RouterLink>
            </li>
            <li class="nav-item" v-if="authStore.user?.roles?.includes('ROLE_ADMIN')">
              <RouterLink class="nav-link" to="/admin">管理后台</RouterLink>
            </li>
          </ul>
          <ul class="navbar-nav align-items-center">
            <li class="nav-item me-2" v-if="authStore.user">
              <span class="badge bg-light text-secondary rounded-pill px-3 py-2 border fw-normal d-flex align-items-center">
                <i class="bi bi-person-circle me-1"></i>
                {{ authStore.user.username }}
              </span>
            </li>
            <li class="nav-item" v-if="!authStore.user">
              <RouterLink class="nav-link" to="/login">登录</RouterLink>
            </li>
            <li class="nav-item" v-if="!authStore.user">
              <RouterLink class="nav-link" to="/register">注册</RouterLink>
            </li>
            <li class="nav-item" v-if="authStore.user">
              <a class="nav-link" href="#" @click.prevent="logout">退出</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  </header>

  <main class="container py-2">
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </main>
</template>

<style>
/* Global styles for the page transition */
.page-enter-active,
.page-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

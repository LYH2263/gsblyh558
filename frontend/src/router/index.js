import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminDashboard.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/practice',
      name: 'practice',
      component: () => import('../views/Practice.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/wrong-questions',
      name: 'wrong-questions',
      component: () => import('../views/WrongQuestions.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/exams',
      name: 'exams',
      component: () => import('../views/ExamList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/exam/:id',
      name: 'take-exam',
      component: () => import('../views/TakeExam.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('../views/403.vue')
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isLoggedIn = !!authStore.user
  const isAdmin = authStore.user?.roles?.includes('ROLE_ADMIN')

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router

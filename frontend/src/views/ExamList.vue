<template>
  <div class="container py-2">
    <div class="d-flex justify-content-between align-items-end mb-5 fade-in">
      <div>
        <h1 class="apple-title mb-1">可用考试</h1>
        <p class="text-secondary mb-0">选择一个考试开始测评您的知识掌握情况</p>
      </div>
    </div>

    <div v-if="loading" class="row g-4">
      <div class="col-md-4" v-for="i in 3" :key="i">
        <div class="card border-0 shadow-sm h-100 overflow-hidden">
          <div class="skeleton-shimmer h-100 p-4">
            <div class="skeleton-title mb-3"></div>
            <div class="skeleton-text mb-2"></div>
            <div class="skeleton-text mb-4 w-75"></div>
            <div class="skeleton-button"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="row g-4">
      <div class="col-md-4" v-for="exam in exams" :key="exam.id">
        <div class="card h-100 border-0 shadow-sm hover-lift fade-in-up">
          <div class="card-body p-4 d-flex flex-column">
            <div class="mb-3">
              <span class="badge bg-primary-soft text-primary mb-2">
                <i class="bi bi-clock me-1"></i>{{ exam.duration }} 分钟
              </span>
              <h5 class="card-title fw-bold mb-3 h4">{{ exam.title }}</h5>
              <p class="card-text text-secondary mb-4 flex-grow-1">{{ exam.description }}</p>
            </div>
            
            <div class="mt-auto pt-3 border-top border-light d-flex justify-content-between align-items-center">
              <span class="text-secondary small">
                <i class="bi bi-star-fill text-warning me-1"></i>总分: {{ exam.totalScore }}
              </span>
              <router-link to="/sessions" class="btn btn-primary px-4">
                <span>预约场次</span>
                <i class="bi bi-arrow-right-short"></i>
              </router-link>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="exams.length === 0" class="col-12 py-5 text-center fade-in">
        <div class="glass-panel d-inline-block p-5 rounded-5" style="min-width: 320px;">
          <div class="mb-4">
            <i class="bi bi-calendar-x display-1 text-primary-soft"></i>
          </div>
          <h3 class="fw-bold mb-2 text-dark">暂无可用考试</h3>
          <p class="text-secondary fs-5 mb-0">目前还没有您可以参加的考试，请稍后再来。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bg-primary-soft {
  background-color: rgba(0, 122, 255, 0.1);
}

.text-primary-soft {
  color: rgba(0, 122, 255, 0.4);
}

.hover-lift {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.hover-lift:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.08) !important;
}

/* Skeleton Shimmer Effect */
.skeleton-shimmer {
  background: #f6f7f8;
  background-image: linear-gradient(to right, #f6f7f8 0%, #edeef1 20%, #f6f7f8 40%, #f6f7f8 100%);
  background-repeat: no-repeat;
  background-size: 800px 100%;
  display: inline-block;
  position: relative;
  animation-duration: 1.5s;
  animation-fill-mode: forwards;
  animation-iteration-count: infinite;
  animation-name: shimmer;
  animation-timing-function: linear;
}

@keyframes shimmer {
  0% { background-position: -468px 0; }
  100% { background-position: 468px 0; }
}

.skeleton-title { height: 24px; border-radius: 4px; background: #eee; width: 60%; }
.skeleton-text { height: 16px; border-radius: 4px; background: #eee; }
.skeleton-button { height: 40px; border-radius: 12px; background: #eee; width: 100px; margin-left: auto; }

.fade-in { animation: fadeIn 0.6s ease-out; }
.fade-in-up { animation: fadeInUp 0.6s cubic-bezier(0.25, 0.8, 0.25, 1) forwards; }

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

<script setup>
import { ref, onMounted } from 'vue'
import { listExams } from '../api/exams'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const exams = ref([])
const loading = ref(true)

const fetchExams = async () => {
  try {
    const response = await listExams()
    exams.value = response.data
  } catch (error) {
    console.error('Failed to fetch exams:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchExams()
})
</script>

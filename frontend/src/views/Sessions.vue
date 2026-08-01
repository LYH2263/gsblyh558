<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAvailableSessions } from '../api/sessions'
import { listMyReservations, createReservation, cancelReservation } from '../api/reservations'
import { useToast } from '../composables/useToast'

const toast = useToast()
const router = useRouter()
const activeTab = ref('available')
const loading = ref(false)
const sessions = ref([])
const reservations = ref([])
const bookingId = ref(null)
const cancellingId = ref(null)

const bookedSessionIds = computed(() => new Set(reservations.value.map(item => item.sessionId)))

const formatDateTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusLabel = (status) => {
  const map = {
    DRAFT: '草稿',
    OPEN: '可预约',
    CLOSED: '已下线',
    FINISHED: '已结束'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    DRAFT: 'bg-light text-secondary',
    OPEN: 'bg-primary-soft text-primary',
    CLOSED: 'bg-danger-soft text-danger',
    FINISHED: 'bg-light text-secondary'
  }
  return map[status] || 'bg-light text-secondary'
}

const getBusinessMessage = (error, fallback) => {
  const errorCode = error.response?.data?.errorCode
  const messages = {
    RSV_CAPACITY_FULL: '该场次名额已满',
    RSV_DUPLICATE_BOOKING: '您已经预约过该场次',
    RSV_BELOW_MIN_ADVANCE: '距离场次开始不足30分钟，无法预约',
    RSV_SESSION_NOT_AVAILABLE: '该场次当前不可预约'
  }
  return messages[errorCode] || error.response?.data?.message || fallback
}

const fetchData = async () => {
  loading.value = true
  try {
    const [sessionRes, reservationRes] = await Promise.all([
      listAvailableSessions(),
      listMyReservations()
    ])
    sessions.value = sessionRes.data || []
    reservations.value = reservationRes.data || []
  } catch (error) {
    console.error('Failed to fetch sessions:', error)
    toast.error('加载场次数据失败')
  } finally {
    loading.value = false
  }
}

const book = async (sessionId) => {
  bookingId.value = sessionId
  try {
    await createReservation(sessionId)
    toast.success('预约成功')
    await fetchData()
    activeTab.value = 'mine'
  } catch (error) {
    console.error('Failed to book session:', error)
    toast.error(getBusinessMessage(error, '预约失败，请稍后重试'))
    await fetchData()
  } finally {
    bookingId.value = null
  }
}

const cancel = async (reservationId) => {
  cancellingId.value = reservationId
  try {
    await cancelReservation(reservationId)
    toast.success('预约已取消')
    await fetchData()
  } catch (error) {
    console.error('Failed to cancel reservation:', error)
    toast.error(error.response?.data?.message || '取消预约失败')
  } finally {
    cancellingId.value = null
  }
}

const enterExam = (reservation) => {
  router.push({
    path: `/exam/${reservation.examId}`,
    query: { reservationId: reservation.id }
  })
}

onMounted(fetchData)
</script>

<template>
  <div class="sessions-page py-2">
    <div class="d-flex justify-content-between align-items-end mb-4 fade-in">
      <div>
        <h1 class="apple-title mb-1">场次预约</h1>
        <p class="text-secondary mb-0">选择合适的考试场次，完成预约后可在我的预约中管理。</p>
      </div>
    </div>

    <div class="glass-panel p-3 p-md-4 mb-4 fade-in-up" style="border-radius: 24px;">
      <ul class="nav nav-pills gap-2 m-0 p-2 bg-light rounded-4 d-inline-flex align-items-center">
        <li class="nav-item">
          <button class="nav-link px-4 py-2 rounded-3 fw-bold" :class="{ active: activeTab === 'available' }" @click="activeTab = 'available'">
            可预约场次
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link px-4 py-2 rounded-3 fw-bold" :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">
            我的预约
            <span class="badge bg-white text-primary ms-2">{{ reservations.length }}</span>
          </button>
        </li>
      </ul>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div v-else-if="activeTab === 'available'" class="row g-4 fade-in">
      <div class="col-md-6 col-xl-4" v-for="session in sessions" :key="session.id">
        <div class="card h-100 border-0 shadow-sm session-card">
          <div class="card-body p-4 d-flex flex-column">
            <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
              <h5 class="fw-bold mb-0">{{ session.examTitle }}</h5>
              <span class="badge" :class="getStatusClass(session.status)">{{ getStatusLabel(session.status) }}</span>
            </div>

            <div class="session-meta mb-4">
              <div class="d-flex align-items-center mb-2 text-secondary">
                <i class="bi bi-calendar-event me-2"></i>
                <span>{{ formatDateTime(session.startTime) }}</span>
              </div>
              <div class="d-flex align-items-center mb-2 text-secondary">
                <i class="bi bi-clock me-2"></i>
                <span>{{ session.durationMinutes }} 分钟</span>
              </div>
              <div class="d-flex align-items-center text-secondary">
                <i class="bi bi-people me-2"></i>
                <span>已预约 {{ session.bookedCount }} / {{ session.capacity }} 人</span>
              </div>
            </div>

            <div class="capacity-bar mb-4">
              <div class="d-flex justify-content-between small mb-2">
                <span class="text-secondary">剩余名额</span>
                <span class="fw-bold" :class="session.remainingCapacity === 0 ? 'text-danger' : 'text-success'">
                  {{ session.remainingCapacity }}
                </span>
              </div>
              <div class="progress" style="height: 8px; border-radius: 999px;">
                <div class="progress-bar" :class="session.remainingCapacity === 0 ? 'bg-danger' : 'bg-success'"
                     :style="{ width: Math.min(100, (session.bookedCount / session.capacity) * 100) + '%' }"></div>
              </div>
            </div>

            <div class="mt-auto d-grid">
              <button v-if="bookedSessionIds.has(session.id)" class="btn btn-secondary" disabled>
                <i class="bi bi-check-circle me-2"></i>已预约
              </button>
              <button v-else-if="session.remainingCapacity === 0" class="btn btn-outline-danger" disabled>
                名额已满
              </button>
              <button v-else class="btn btn-primary" :disabled="bookingId === session.id" @click="book(session.id)">
                <span v-if="bookingId === session.id">预约中...</span>
                <span v-else>立即预约</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="sessions.length === 0" class="col-12 py-5 text-center">
        <div class="glass-panel d-inline-block p-5 rounded-5" style="min-width: 320px;">
          <i class="bi bi-calendar-x display-1 text-primary-soft d-block mb-3"></i>
          <h3 class="fw-bold mb-2">暂无可预约场次</h3>
          <p class="text-secondary fs-5 mb-0">当前没有开放预约的考试场次，请稍后再来。</p>
        </div>
      </div>
    </div>

    <div v-else class="fade-in">
      <div class="glass-panel p-4 p-md-5" style="border-radius: 24px;">
        <div v-if="reservations.length === 0" class="text-center py-5">
          <i class="bi bi-journal-text display-1 text-primary-soft d-block mb-3"></i>
          <h3 class="fw-bold mb-2">暂无预约</h3>
          <p class="text-secondary fs-5 mb-0">去可预约场次列表选择一场考试吧。</p>
          <button class="btn btn-primary mt-4" @click="activeTab = 'available'">查看可预约场次</button>
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>考试</th>
                <th>开始时间</th>
                <th>时长</th>
                <th>预约时间</th>
                <th>状态</th>
                <th class="text-end">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="reservation in reservations" :key="reservation.id">
                <td class="fw-bold">{{ reservation.examTitle }}</td>
                <td class="text-secondary">{{ formatDateTime(reservation.sessionStartTime) }}</td>
                <td class="text-secondary">{{ reservation.durationMinutes }} 分钟</td>
                <td class="text-secondary">{{ formatDateTime(reservation.reservedAt) }}</td>
                <td>
                  <span class="badge" :class="getStatusClass(reservation.sessionStatus)">
                    {{ getStatusLabel(reservation.sessionStatus) }}
                  </span>
                </td>
                <td class="text-end">
                  <div class="d-flex gap-2 justify-content-end">
                    <button class="btn btn-sm btn-light text-primary px-3" @click="enterExam(reservation)">
                      进入考试
                    </button>
                    <button class="btn btn-sm btn-light text-danger px-3"
                            :disabled="cancellingId === reservation.id"
                            @click="cancel(reservation.id)">
                      {{ cancellingId === reservation.id ? '取消中...' : '取消预约' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bg-primary-soft {
  background-color: rgba(0, 122, 255, 0.1);
}

.bg-danger-soft {
  background-color: rgba(255, 59, 48, 0.1);
}

.text-primary-soft {
  color: rgba(0, 122, 255, 0.4);
}

.session-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.session-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.1) !important;
}

.nav-pills .nav-link {
  color: var(--text-secondary);
}

.nav-pills .nav-link.active {
  background-color: white;
  color: var(--apple-blue);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.fade-in {
  animation: fadeIn 0.5s ease-out;
}

.fade-in-up {
  animation: fadeInUp 0.5s cubic-bezier(0.25, 0.8, 0.25, 1) both;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

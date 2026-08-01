<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '../composables/useToast'
import {
  fetchOpenSessions,
  bookSession,
  cancelReservation,
  fetchMyReservations,
  checkSessionAccess
} from '../api/reservation'
import { resolveReservationError } from '../api/reservationErrors'

const toast = useToast()
const router = useRouter()

const activeTab = ref('available')
const loading = ref(true)
const sessions = ref([])
const myReservations = ref([])
const submittingId = ref(null)

const loadSessions = async () => {
  try {
    const res = await fetchOpenSessions()
    sessions.value = res.data.data
  } catch (error) {
    toast.error(resolveReservationError(error, '获取可预约场次失败'))
  }
}

const loadMyReservations = async () => {
  try {
    const res = await fetchMyReservations()
    myReservations.value = res.data.data
  } catch (error) {
    toast.error(resolveReservationError(error, '获取我的预约失败'))
  }
}

const refreshAll = async () => {
  loading.value = true
  await Promise.all([loadSessions(), loadMyReservations()])
  loading.value = false
}

const activeReservationCount = computed(
  () => myReservations.value.filter(r => r.status === 'BOOKED').length
)

const handleBook = async (session) => {
  submittingId.value = session.id
  try {
    await bookSession(session.id)
    toast.success('预约成功')
    await refreshAll()
  } catch (error) {
    toast.error(resolveReservationError(error, '预约失败'))
  } finally {
    submittingId.value = null
  }
}

const handleCancel = async (reservation) => {
  submittingId.value = reservation.id
  try {
    await cancelReservation(reservation.id)
    toast.success('已取消预约')
    await refreshAll()
  } catch (error) {
    toast.error(resolveReservationError(error, '取消失败'))
  } finally {
    submittingId.value = null
  }
}

const formatTime = (value) => {
  if (!value) return '--'
  const d = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const statusLabel = (status) => (status === 'BOOKED' ? '已预约' : '已取消')

// 进入考试前先做开考准入校验，通过后携带场次/预约进入答题页
const handleEnter = async (reservation) => {
  submittingId.value = reservation.id
  try {
    const res = await checkSessionAccess(reservation.sessionId)
    const access = res.data.data
    router.push({
      name: 'take-exam',
      params: { id: access.examId },
      query: {
        sessionId: access.sessionId,
        reservationId: access.reservationId,
        remainingSeconds: access.remainingSeconds
      }
    })
  } catch (error) {
    // 未预约 / 未开始 / 已结束 / 已取消 四种情况按错误码展示不同文案
    toast.error(resolveReservationError(error, '暂时无法进入考试'))
  } finally {
    submittingId.value = null
  }
}

onMounted(refreshAll)
</script>

<template>
  <div class="container py-2">
    <div class="d-flex justify-content-between align-items-end mb-5 fade-in">
      <div>
        <h1 class="apple-title mb-1">场次预约</h1>
        <p class="text-secondary mb-0">选择合适的考试场次进行预约，名额有限先到先得</p>
      </div>
    </div>

    <div class="glass-panel p-4 p-md-5 fade-in-up" style="border-radius: 32px;">
      <ul class="nav nav-pills gap-2 mb-5 p-2 bg-light rounded-4 d-inline-flex align-items-center">
        <li class="nav-item">
          <button class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center"
                  :class="{ active: activeTab === 'available' }" @click="activeTab = 'available'">
            可预约场次
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center"
                  :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">
            我的预约
            <span v-if="activeReservationCount > 0" class="badge bg-primary-soft text-primary ms-2">{{ activeReservationCount }}</span>
          </button>
        </li>
      </ul>

      <!-- Loading skeleton -->
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

      <!-- Available sessions -->
      <div v-else-if="activeTab === 'available'" class="row g-4 fade-in">
        <div class="col-md-4" v-for="session in sessions" :key="session.id">
          <div class="card h-100 border-0 shadow-sm hover-lift">
            <div class="card-body p-4 d-flex flex-column">
              <div class="mb-3">
                <span class="badge bg-primary-soft text-primary mb-2">
                  <i class="bi bi-clock me-1"></i>{{ session.durationMinutes }} 分钟
                </span>
                <h5 class="card-title fw-bold mb-2 h5">{{ session.examTitle }}</h5>
                <p class="card-text text-secondary small mb-3" style="min-height: 40px;">{{ session.examDescription || '暂无描述' }}</p>
                <div class="d-flex align-items-center text-secondary small mb-2">
                  <i class="bi bi-calendar-event me-2"></i>{{ formatTime(session.startTime) }}
                </div>
              </div>

              <div class="mt-auto pt-3 border-top border-light">
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-secondary small">
                    <i class="bi bi-people me-1"></i>剩余名额
                  </span>
                  <span class="fw-bold" :class="session.remainingSeats > 0 ? 'text-success' : 'text-danger'">
                    {{ session.remainingSeats }} / {{ session.capacity }}
                  </span>
                </div>
                <button v-if="session.reservedByMe" class="btn btn-outline-success w-100" disabled>
                  <i class="bi bi-check-circle me-1"></i>已预约
                </button>
                <button v-else class="btn btn-primary w-100"
                        :disabled="session.remainingSeats <= 0 || submittingId === session.id"
                        @click="handleBook(session)">
                  <span v-if="submittingId === session.id" class="spinner-border spinner-border-sm me-2"></span>
                  <span v-if="session.remainingSeats <= 0">名额已满</span>
                  <span v-else>立即预约</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="sessions.length === 0" class="col-12 py-5 text-center fade-in">
          <div class="glass-panel d-inline-block p-5 rounded-5" style="min-width: 320px;">
            <div class="mb-4"><i class="bi bi-calendar-x display-1 text-primary-soft"></i></div>
            <h3 class="fw-bold mb-2 text-dark">暂无可预约场次</h3>
            <p class="text-secondary fs-5 mb-0">目前还没有开放预约的考试场次，请稍后再来。</p>
          </div>
        </div>
      </div>

      <!-- My reservations -->
      <div v-else class="fade-in">
        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>考试</th>
                <th width="180">开始时间</th>
                <th width="100">时长</th>
                <th width="180">预约时间</th>
                <th width="100">状态</th>
                <th width="120">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in myReservations" :key="r.id">
                <td class="fw-bold">{{ r.examTitle }}</td>
                <td><span class="text-secondary small"><i class="bi bi-calendar-event me-1"></i>{{ formatTime(r.startTime) }}</span></td>
                <td><span class="text-secondary small">{{ r.durationMinutes }} 分钟</span></td>
                <td><span class="text-secondary small">{{ formatTime(r.reservedAt) }}</span></td>
                <td>
                  <span class="badge" :class="r.status === 'BOOKED' ? 'bg-success-soft text-success' : 'bg-secondary-soft text-secondary'">
                    {{ statusLabel(r.status) }}
                  </span>
                </td>
                <td>
                  <div v-if="r.status === 'BOOKED'" class="d-flex gap-2">
                    <button class="btn btn-sm btn-primary px-3"
                            :disabled="submittingId === r.id" @click="handleEnter(r)">
                      <span v-if="submittingId === r.id" class="spinner-border spinner-border-sm me-1"></span>进入考试
                    </button>
                    <button class="btn btn-sm btn-light text-danger px-3"
                            :disabled="submittingId === r.id" @click="handleCancel(r)">取消</button>
                  </div>
                  <span v-else class="text-secondary small">--</span>
                </td>
              </tr>
              <tr v-if="myReservations.length === 0">
                <td colspan="6" class="text-center py-5 text-secondary">
                  <i class="bi bi-inbox display-6 d-block mb-3 opacity-25"></i>
                  <p class="small mb-0">您还没有任何预约记录</p>
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
.bg-primary-soft { background-color: rgba(0, 122, 255, 0.1); }
.text-primary-soft { color: rgba(0, 122, 255, 0.4); }
.bg-success-soft { background-color: rgba(52, 199, 89, 0.1); }
.bg-secondary-soft { background-color: rgba(142, 142, 147, 0.12); }

.hover-lift { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.hover-lift:hover { transform: translateY(-8px); box-shadow: 0 12px 24px rgba(0,0,0,0.08) !important; }

.nav-pills .nav-link { color: var(--text-secondary); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.nav-pills .nav-link.active { background-color: white; color: var(--apple-blue); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

.skeleton-shimmer {
  background: #f6f7f8;
  background-image: linear-gradient(to right, #f6f7f8 0%, #edeef1 20%, #f6f7f8 40%, #f6f7f8 100%);
  background-repeat: no-repeat;
  background-size: 800px 100%;
  display: inline-block;
  position: relative;
  animation: shimmer 1.5s infinite linear forwards;
}
@keyframes shimmer { 0% { background-position: -468px 0; } 100% { background-position: 468px 0; } }
.skeleton-title { height: 24px; border-radius: 4px; background: #eee; width: 60%; }
.skeleton-text { height: 16px; border-radius: 4px; background: #eee; }
.skeleton-button { height: 40px; border-radius: 12px; background: #eee; width: 100px; }

.fade-in { animation: fadeIn 0.6s ease-out; }
.fade-in-up { animation: fadeInUp 0.6s cubic-bezier(0.25, 0.8, 0.25, 1) forwards; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>

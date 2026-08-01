<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOpenSessions, bookSession } from '../api/sessions'
import { listMyReservations, cancelReservation } from '../api/reservations'
import { useToast } from '../composables/useToast'
import { getErrorMessage } from '../utils/reservationMessages'

const router = useRouter()
const toast = useToast()
const activeTab = ref('sessions')
const sessions = ref([])
const myReservations = ref([])
const loadingSessions = ref(false)
const loadingReservations = ref(false)
const bookingIds = ref(new Set())
const cancellingIds = ref(new Set())

const myBookedSessionIds = computed(() =>
  new Set(myReservations.value.map(r => r.sessionId))
)

const isWithinEntryWindow = (reservation) => {
  if (!reservation?.sessionStartTime) return false
  const start = new Date(reservation.sessionStartTime).getTime()
  const grace = (reservation.sessionLateGraceMinutes ?? 10) * 60 * 1000
  const now = Date.now()
  return now >= start && now <= start + grace
}

const isFutureSession = (reservation) => {
  if (!reservation?.sessionStartTime) return false
  return Date.now() < new Date(reservation.sessionStartTime).getTime()
}

const enterExam = (reservation) => {
  router.push(`/sessions/${reservation.sessionId}/take`)
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const fetchSessions = async () => {
  loadingSessions.value = true
  try {
    const res = await listOpenSessions()
    sessions.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch sessions:', error)
    toast.error(getErrorMessage(error, '加载可预约场次失败'))
  } finally {
    loadingSessions.value = false
  }
}

const fetchMyReservations = async () => {
  loadingReservations.value = true
  try {
    const res = await listMyReservations()
    myReservations.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch reservations:', error)
    toast.error(getErrorMessage(error, '加载我的预约失败'))
  } finally {
    loadingReservations.value = false
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'sessions' && sessions.value.length === 0) {
    fetchSessions()
  }
  if (tab === 'mine' && myReservations.value.length === 0) {
    fetchMyReservations()
  }
}

const handleBook = async (session) => {
  if (bookingIds.value.has(session.id)) return
  if (myBookedSessionIds.value.has(session.id)) {
    toast.warning('您已经预约过该场次')
    return
  }
  if (session.remainingSlots <= 0) {
    toast.warning('该场次名额已满')
    return
  }
  bookingIds.value.add(session.id)
  try {
    await bookSession(session.id)
    toast.success('预约成功')
    await Promise.all([fetchSessions(), fetchMyReservations()])
  } catch (error) {
    console.error('Failed to book session:', error)
    toast.error(getErrorMessage(error, '预约失败'))
  } finally {
    bookingIds.value.delete(session.id)
  }
}

const handleCancel = async (reservation) => {
  if (cancellingIds.value.has(reservation.id)) return
  cancellingIds.value.add(reservation.id)
  try {
    await cancelReservation(reservation.id)
    toast.success('预约已取消')
    await Promise.all([fetchSessions(), fetchMyReservations()])
  } catch (error) {
    console.error('Failed to cancel reservation:', error)
    toast.error(getErrorMessage(error, '取消预约失败'))
  } finally {
    cancellingIds.value.delete(reservation.id)
  }
}

onMounted(() => {
  fetchSessions()
  fetchMyReservations()
})
</script>

<template>
  <div class="session-booking py-2">
    <div class="d-flex justify-content-between align-items-end mb-4 fade-in">
      <div>
        <h1 class="apple-title mb-1">场次预约</h1>
        <p class="text-secondary mb-0">浏览开放中的考试场次，选择合适的时间完成预约</p>
      </div>
    </div>

    <div class="glass-panel p-4 p-md-5 fade-in-up" style="border-radius: 28px;">
      <ul class="nav nav-pills gap-2 mb-4 p-2 bg-light rounded-4 d-inline-flex align-items-center">
        <li class="nav-item">
          <button
            class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center"
            :class="{ active: activeTab === 'sessions' }"
            @click="switchTab('sessions')"
          >
            <i class="bi bi-calendar-check me-2"></i>可预约场次
          </button>
        </li>
        <li class="nav-item">
          <button
            class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center"
            :class="{ active: activeTab === 'mine' }"
            @click="switchTab('mine')"
          >
            <i class="bi bi-journal-bookmark me-2"></i>我的预约
            <span class="badge bg-primary ms-2" v-if="myReservations.length">{{ myReservations.length }}</span>
          </button>
        </li>
      </ul>

      <div v-if="activeTab === 'sessions'">
        <div v-if="loadingSessions" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="text-secondary mt-3">正在加载场次...</p>
        </div>

        <div v-else-if="sessions.length === 0" class="text-center py-5">
          <i class="bi bi-calendar-x display-1 d-block text-secondary opacity-25 mb-3"></i>
          <h4 class="fw-bold mb-2">暂无可预约场次</h4>
          <p class="text-secondary mb-0">当前没有开放预约的场次，请稍后再查看。</p>
        </div>

        <div v-else class="row g-4">
          <div class="col-md-6 col-xl-4" v-for="session in sessions" :key="session.id">
            <div class="card h-100 border-0 shadow-sm session-card">
              <div class="card-body p-4 d-flex flex-column">
                <div class="d-flex justify-content-between align-items-start mb-3">
                  <span class="badge bg-primary-soft text-primary px-3 py-2">
                    <i class="bi bi-clock me-1"></i>{{ session.durationMinutes }} 分钟
                  </span>
                  <span
                    class="badge px-3 py-2"
                    :class="session.remainingSlots > 0 ? 'bg-success-soft text-success' : 'bg-danger-soft text-danger'"
                  >
                    剩余 {{ session.remainingSlots }} / {{ session.capacity }}
                  </span>
                </div>
                <h5 class="fw-bold mb-3">{{ session.examTitle }}</h5>
                <div class="text-secondary small mb-4 flex-grow-1">
                  <div class="d-flex align-items-center mb-2">
                    <i class="bi bi-calendar-event me-2"></i>
                    <span>{{ formatDateTime(session.startTime) }}</span>
                  </div>
                  <div class="d-flex align-items-center">
                    <i class="bi bi-people me-2"></i>
                    <span>已预约 {{ session.bookedCount }} 人</span>
                  </div>
                </div>
                <div class="mt-auto d-grid">
                  <button
                    v-if="myBookedSessionIds.has(session.id)"
                    class="btn btn-secondary"
                    disabled
                  >
                    <i class="bi bi-check-circle me-2"></i>已预约
                  </button>
                  <button
                    v-else-if="session.remainingSlots <= 0"
                    class="btn btn-secondary"
                    disabled
                  >
                    名额已满
                  </button>
                  <button
                    v-else
                    class="btn btn-primary"
                    :disabled="bookingIds.has(session.id)"
                    @click="handleBook(session)"
                  >
                    <span v-if="bookingIds.has(session.id)">
                      <span class="spinner-border spinner-border-sm me-2"></span>预约中...
                    </span>
                    <span v-else>
                      <i class="bi bi-bookmark-plus me-2"></i>立即预约
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else>
        <div v-if="loadingReservations" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="text-secondary mt-3">正在加载预约记录...</p>
        </div>

        <div v-else-if="myReservations.length === 0" class="text-center py-5">
          <i class="bi bi-journal-x display-1 d-block text-secondary opacity-25 mb-3"></i>
          <h4 class="fw-bold mb-2">暂无预约记录</h4>
          <p class="text-secondary mb-0">去“可预约场次”里选择一场考试吧。</p>
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>考试</th>
                <th>开始时间</th>
                <th>考试时长</th>
                <th>预约时间</th>
                <th>状态</th>
                <th class="text-end">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="reservation in myReservations" :key="reservation.id">
                <td class="fw-bold">{{ reservation.examTitle }}</td>
                <td>{{ formatDateTime(reservation.sessionStartTime) }}</td>
                <td>{{ reservation.sessionDurationMinutes }} 分钟</td>
                <td>{{ formatDateTime(reservation.reservedAt) }}</td>
                <td>
                  <span class="badge bg-success-soft text-success">已预约</span>
                </td>
                <td class="text-end">
                  <div class="d-inline-flex gap-2">
                    <button
                      v-if="isWithinEntryWindow(reservation)"
                      class="btn btn-sm btn-primary px-3"
                      @click="enterExam(reservation)"
                    >
                      进入考试
                    </button>
                    <span
                      v-else-if="isFutureSession(reservation)"
                      class="small text-secondary align-self-center"
                    >
                      未到开始时间
                    </span>
                    <span
                      v-else
                      class="small text-secondary align-self-center"
                    >
                      已过入场时间
                    </span>
                    <button
                      class="btn btn-sm btn-light text-danger px-3"
                      :disabled="cancellingIds.has(reservation.id)"
                      @click="handleCancel(reservation)"
                    >
                      <span v-if="cancellingIds.has(reservation.id)">
                        <span class="spinner-border spinner-border-sm me-1"></span>取消中
                      </span>
                      <span v-else>取消预约</span>
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

.bg-success-soft {
  background-color: rgba(52, 199, 89, 0.1);
  color: #1a7d32;
}

.bg-danger-soft {
  background-color: rgba(255, 59, 48, 0.1);
  color: #d32f2f;
}

.session-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.session-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08) !important;
}

.nav-pills .nav-link {
  color: var(--text-secondary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-pills .nav-link.active {
  background-color: white;
  color: var(--apple-blue);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

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

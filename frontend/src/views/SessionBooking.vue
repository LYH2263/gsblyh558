<script setup>
import { ref, onMounted } from 'vue'
import { sessionApi } from '../api/sessions'
import { reservationApi } from '../api/reservations'
import { useToast } from '../composables/useToast'

const toast = useToast()
const activeTab = ref('bookable')
const loading = ref(true)
const sessions = ref([])
const myReservations = ref([])
const bookingSessionId = ref(null)
const cancellingId = ref(null)

// A5: 根据后端 RSV_ 业务错误码展示中文提示，不做后端文案字符串匹配
const RSV_ERROR_MESSAGES = {
  RSV_SESSION_FULL: '该场次名额已满，请选择其他场次',
  RSV_DUPLICATE_RESERVATION: '您已预约过该场次，不能重复预约',
  RSV_TOO_CLOSE_TO_START: '距场次开始不足 30 分钟，无法预约',
  RSV_SESSION_NOT_OPEN: '该场次未开放预约',
  RSV_SESSION_NOT_FOUND: '场次不存在或已下线',
  RSV_RESERVATION_NOT_FOUND: '预约记录不存在',
  RSV_RESERVATION_NOT_ACTIVE: '该预约已取消，无法重复操作',
  RSV_NOT_RESERVED: '您尚未预约该场次，请先完成预约',
  RSV_RESERVATION_CANCELLED: '您的预约已取消，无法进入考试',
  RSV_SESSION_NOT_STARTED: '该场次尚未开始，请稍后再来',
  RSV_SESSION_ENDED: '该场次已结束，无法进入考试',
  RSV_SUBMIT_TIMEOUT: '已超出作答窗口，本次作答已按超时交卷处理'
}

const rsvErrorMessage = (error) => {
  const data = error.response?.data
  if (data?.errorCode && RSV_ERROR_MESSAGES[data.errorCode]) {
    return RSV_ERROR_MESSAGES[data.errorCode]
  }
  return data?.message || error.message
}

const formatDateTime = (iso) => {
  if (!iso) return '-'
  return iso.replace('T', ' ').slice(0, 16)
}

const fetchSessions = async () => {
  try {
    const res = await sessionApi.listBookable()
    sessions.value = res.data.data
  } catch (error) {
    console.error('Failed to fetch sessions:', error)
    toast.error('加载场次失败')
  }
}

const fetchMyReservations = async () => {
  try {
    const res = await reservationApi.my()
    myReservations.value = res.data.data
  } catch (error) {
    console.error('Failed to fetch reservations:', error)
    toast.error('加载我的预约失败')
  }
}

const bookSession = async (session) => {
  if (bookingSessionId.value) return
  bookingSessionId.value = session.id
  try {
    await reservationApi.book(session.id)
    toast.success('预约成功！可在「我的预约」中查看')
    await Promise.all([fetchSessions(), fetchMyReservations()])
  } catch (error) {
    console.error('Failed to book session:', error)
    toast.error(rsvErrorMessage(error))
  } finally {
    bookingSessionId.value = null
  }
}

const cancelReservation = async (reservation) => {
  if (cancellingId.value) return
  cancellingId.value = reservation.id
  try {
    await reservationApi.cancel(reservation.id)
    toast.success('预约已取消，名额已释放')
    await Promise.all([fetchSessions(), fetchMyReservations()])
  } catch (error) {
    console.error('Failed to cancel reservation:', error)
    toast.error(rsvErrorMessage(error))
  } finally {
    cancellingId.value = null
  }
}

const bookButtonText = (session) => {
  if (bookingSessionId.value === session.id) return '预约中...'
  if (session.remainingQuota <= 0) return '已满员'
  if (session.minutesUntilStart < 30) return '即将开始'
  return '立即预约'
}

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchSessions(), fetchMyReservations()])
  loading.value = false
})
</script>

<template>
  <div class="session-booking position-relative py-2">
    <div class="d-flex justify-content-between align-items-end mb-5 fade-in">
      <div>
        <h1 class="apple-title mb-1">场次预约</h1>
        <p class="text-secondary mb-0">浏览可预约的考试场次，提前预约您的考试时间</p>
      </div>
    </div>

    <ul class="nav nav-pills gap-2 mb-5 p-2 bg-light rounded-4 d-inline-flex align-items-center fade-in">
      <li class="nav-item">
        <button class="nav-link px-4 py-2 rounded-3 fw-bold" :class="{ active: activeTab === 'bookable' }" @click="activeTab = 'bookable'">可预约场次</button>
      </li>
      <li class="nav-item">
        <button class="nav-link px-4 py-2 rounded-3 fw-bold" :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">
          我的预约
          <span v-if="myReservations.filter(r => r.status === 'BOOKED').length > 0" class="badge bg-primary-soft text-primary ms-1">
            {{ myReservations.filter(r => r.status === 'BOOKED').length }}
          </span>
        </button>
      </li>
    </ul>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- 可预约场次 -->
    <div v-else-if="activeTab === 'bookable'" class="row g-4 fade-in">
      <div class="col-md-6 col-lg-4" v-for="session in sessions" :key="session.id">
        <div class="card h-100 border-0 shadow-sm hover-lift fade-in-up">
          <div class="card-body p-4 d-flex flex-column">
            <div class="mb-3">
              <div class="d-flex gap-2 mb-2">
                <span class="badge bg-primary-soft text-primary">
                  <i class="bi bi-clock me-1"></i>{{ session.durationMinutes }} 分钟
                </span>
                <span class="badge" :class="session.remainingQuota > 0 ? 'bg-success-soft text-success' : 'bg-danger-soft text-danger'">
                  剩余名额 {{ session.remainingQuota }}
                </span>
              </div>
              <h5 class="card-title fw-bold mb-2 h4">{{ session.examTitle }}</h5>
              <p class="card-text text-secondary mb-1">
                <i class="bi bi-calendar-event me-1"></i>开始时间：{{ formatDateTime(session.startTime) }}
              </p>
              <p class="card-text text-secondary small mb-0">
                <i class="bi bi-people me-1"></i>已预约 {{ session.bookedCount }} / {{ session.capacity }} 人
              </p>
            </div>

            <div class="mt-auto pt-3 border-top border-light">
              <button
                class="btn w-100 py-2"
                :class="session.bookable ? 'btn-primary' : 'btn-secondary disabled'"
                :disabled="!session.bookable || bookingSessionId === session.id"
                @click="bookSession(session)"
              >
                <span v-if="bookingSessionId === session.id" class="spinner-border spinner-border-sm me-2"></span>
                {{ bookButtonText(session) }}
              </button>
              <p v-if="session.remainingQuota > 0 && session.minutesUntilStart < 30" class="text-secondary small text-center mt-2 mb-0">
                距开始不足 30 分钟，停止预约
              </p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="sessions.length === 0" class="col-12 py-5 text-center fade-in">
        <div class="glass-panel d-inline-block p-5 rounded-5" style="min-width: 320px;">
          <div class="mb-4">
            <i class="bi bi-calendar-x display-1 text-primary-soft"></i>
          </div>
          <h3 class="fw-bold mb-2 text-dark">暂无可预约场次</h3>
          <p class="text-secondary fs-5 mb-0">目前还没有开放预约的考试场次，请稍后再来。</p>
        </div>
      </div>
    </div>

    <!-- 我的预约 -->
    <div v-else class="fade-in">
      <div class="glass-panel p-4 rounded-5">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>考试</th>
                <th width="170">开始时间</th>
                <th width="110">时长</th>
                <th width="170">预约时间</th>
                <th width="110">状态</th>
                <th width="210">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in myReservations" :key="item.id">
                <td class="fw-bold">{{ item.examTitle }}</td>
                <td><span class="text-secondary small"><i class="bi bi-calendar-event me-1"></i>{{ formatDateTime(item.startTime) }}</span></td>
                <td><span class="text-secondary small"><i class="bi bi-clock me-1"></i>{{ item.durationMinutes }} 分钟</span></td>
                <td><span class="text-secondary small">{{ formatDateTime(item.reservedAt) }}</span></td>
                <td>
                  <span class="badge" :class="item.status === 'BOOKED' ? 'bg-success-soft text-success' : 'bg-light text-secondary'">
                    {{ item.status === 'BOOKED' ? '已预约' : '已取消' }}
                  </span>
                </td>
                <td>
                  <div class="d-flex gap-2">
                    <router-link
                      v-if="item.status === 'BOOKED'"
                      class="btn btn-sm btn-primary px-3"
                      :to="`/exam/${item.examId}?sessionId=${item.sessionId}`"
                    >进入考试</router-link>
                    <button
                      v-if="item.status === 'BOOKED'"
                      class="btn btn-sm btn-light text-danger px-3"
                      :disabled="cancellingId === item.id"
                      @click="cancelReservation(item)"
                    >
                      <span v-if="cancellingId === item.id" class="spinner-border spinner-border-sm me-1"></span>
                      取消预约
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="myReservations.length === 0">
                <td colspan="6" class="text-center text-secondary py-5">
                  <i class="bi bi-inbox d-block display-6 mb-3 opacity-25"></i>您还没有任何预约记录
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
}

.bg-danger-soft {
  background-color: rgba(255, 59, 48, 0.1);
}

.text-primary-soft {
  color: rgba(0, 122, 255, 0.4);
}

.hover-lift {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.hover-lift:hover {
  transform: translateY(-8px);
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

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  listAdminSessions,
  createAdminSession,
  updateAdminSession,
  offlineAdminSession,
  listSessionReservations
} from '../api/adminSessions'
import { listExams } from '../api/exams'
import { useToast } from '../composables/useToast'
import { getErrorMessage } from '../utils/reservationMessages'

const toast = useToast()

const sessions = ref([])
const exams = ref([])
const loading = ref(false)
const showSessionModal = ref(false)
const showReservationsModal = ref(false)
const reservations = ref([])
const reservationsLoading = ref(false)
const saving = ref(false)
const offliningIds = ref(new Set())
const statusFilter = ref('')

const defaultForm = () => ({
  id: null,
  examId: null,
  startTime: '',
  durationHours: 1,
  capacity: 50,
  lateGraceMinutes: 10,
  screenSwitchLimit: 3,
  status: 'DRAFT'
})

const form = ref(defaultForm())
const editingId = ref(null)

const statusLabels = {
  DRAFT: '草稿',
  OPEN: '开放预约',
  CLOSED: '已下线',
  FINISHED: '已结束'
}

const statusBadgeClass = {
  DRAFT: 'bg-secondary',
  OPEN: 'bg-success-soft text-success',
  CLOSED: 'bg-danger-soft text-danger',
  FINISHED: 'bg-light text-secondary'
}

const modalTitle = computed(() => (editingId.value ? '编辑场次' : '新建场次'))

const formatDateTimeLocal = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const fetchSessions = async () => {
  loading.value = true
  try {
    const params = statusFilter.value ? { status: statusFilter.value } : {}
    const res = await listAdminSessions(params)
    sessions.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch sessions:', error)
    toast.error(getErrorMessage(error, '加载场次失败'))
  } finally {
    loading.value = false
  }
}

const hoursToMinutes = (hours) => {
  const n = Number(hours)
  if (!Number.isFinite(n) || n <= 0) return null
  return Math.round(n * 60)
}

const minutesToHours = (minutes) => {
  if (!minutes) return ''
  return (minutes / 60).toString()
}

watch(statusFilter, () => {
  fetchSessions()
})

const fetchExams = async () => {
  try {
    const res = await listExams()
    exams.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch exams:', error)
    toast.error(getErrorMessage(error, '加载考试列表失败'))
  }
}

const openCreate = () => {
  form.value = defaultForm()
  if (exams.value.length > 0) {
    form.value.examId = exams.value[0].id
  }
  editingId.value = null
  showSessionModal.value = true
}

const openEdit = (session) => {
  form.value = {
    id: session.id,
    examId: session.examId,
    startTime: formatDateTimeLocal(session.startTime),
    durationHours: minutesToHours(session.durationMinutes),
    capacity: session.capacity,
    lateGraceMinutes: session.lateGraceMinutes ?? 10,
    screenSwitchLimit: session.screenSwitchLimit ?? 3,
    status: session.status
  }
  editingId.value = session.id
  showSessionModal.value = true
}

const closeModal = () => {
  showSessionModal.value = false
}

const saveSession = async () => {
  if (!form.value.examId) {
    toast.warning('请选择关联考试')
    return
  }
  if (!form.value.startTime) {
    toast.warning('请选择开始时间')
    return
  }
  const durationMinutes = hoursToMinutes(form.value.durationHours)
  if (!durationMinutes || durationMinutes <= 0) {
    toast.warning('考试时长必须大于 0 小时')
    return
  }
  if (!form.value.capacity || form.value.capacity <= 0) {
    toast.warning('最大预约人数必须大于 0')
    return
  }
  if (form.value.lateGraceMinutes == null || form.value.lateGraceMinutes < 0) {
    toast.warning('迟到宽限量不能为负数')
    return
  }
  if (!form.value.screenSwitchLimit || form.value.screenSwitchLimit < 1) {
    toast.warning('切屏强制交卷阈值必须大于 0')
    return
  }

  saving.value = true
  try {
    const payload = {
      examId: form.value.examId,
      startTime: new Date(form.value.startTime).toISOString(),
      durationMinutes,
      capacity: Number(form.value.capacity),
      lateGraceMinutes: Number(form.value.lateGraceMinutes),
      screenSwitchLimit: Number(form.value.screenSwitchLimit),
      status: form.value.status
    }
    if (editingId.value) {
      const updatePayload = {
        startTime: payload.startTime,
        durationMinutes: payload.durationMinutes,
        capacity: payload.capacity,
        lateGraceMinutes: payload.lateGraceMinutes,
        screenSwitchLimit: payload.screenSwitchLimit,
        status: payload.status
      }
      await updateAdminSession(editingId.value, updatePayload)
      toast.success('场次更新成功')
    } else {
      await createAdminSession(payload)
      toast.success('场次创建成功')
    }
    showSessionModal.value = false
    fetchSessions()
  } catch (error) {
    console.error('Failed to save session:', error)
    toast.error(getErrorMessage(error, '保存场次失败'))
  } finally {
    saving.value = false
  }
}

const offlineSession = (session) => {
  if (offliningIds.value.has(session.id)) return
  const hasBookings = session.bookedCount > 0
  const message = hasBookings
    ? `该场次当前已有 ${session.bookedCount} 人预约，下线后这些预约将全部取消，是否继续？`
    : '确定要下线该场次吗？'
  if (!window.confirm(message)) return
  offliningIds.value.add(session.id)
  offlineAdminSession(session.id)
    .then((res) => {
      toast.success(res.message || '场次已下线')
      fetchSessions()
    })
    .catch((error) => {
      console.error('Failed to offline session:', error)
      toast.error(getErrorMessage(error, '下线场次失败'))
    })
    .finally(() => {
      offliningIds.value.delete(session.id)
    })
}

const viewReservations = async (session) => {
  showReservationsModal.value = true
  reservationsLoading.value = true
  reservations.value = []
  try {
    const res = await listSessionReservations(session.id)
    reservations.value = (res.data || []).filter(r => r.status === 'BOOKED')
  } catch (error) {
    console.error('Failed to fetch reservations:', error)
    toast.error(getErrorMessage(error, '加载预约名单失败'))
  } finally {
    reservationsLoading.value = false
  }
}

watch(showReservationsModal, (value) => {
  if (!value) {
    reservations.value = []
  }
})

onMounted(() => {
  fetchExams()
  fetchSessions()
})
</script>

<template>
  <div class="admin-sessions">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
      <h3 class="fw-bold h4 mb-0">场次管理</h3>
      <div class="d-flex align-items-center gap-2">
        <label class="small text-secondary mb-0">状态筛选</label>
        <select v-model="statusFilter" class="form-select form-select-sm" style="width: 140px;">
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="OPEN">开放预约</option>
          <option value="CLOSED">已下线</option>
          <option value="FINISHED">已结束</option>
        </select>
        <button class="btn btn-primary px-4 py-2 shadow-sm" @click="openCreate">
          <i class="bi bi-plus-lg me-2"></i>新建场次
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
      <p class="text-secondary mt-3">正在加载场次...</p>
    </div>

    <div v-else-if="sessions.length === 0" class="text-center py-5">
      <i class="bi bi-calendar-x display-1 d-block text-secondary opacity-25 mb-3"></i>
      <h4 class="fw-bold mb-2">暂无场次</h4>
      <p class="text-secondary mb-0">点击右上角“新建场次”开始创建考试场次。</p>
    </div>

    <div v-else class="table-responsive">
      <table class="table table-hover align-middle">
        <thead>
          <tr>
            <th width="70">ID</th>
            <th>所属考试</th>
            <th>开始时间</th>
            <th width="100">时长</th>
            <th width="100">已预约</th>
            <th width="100">容量</th>
            <th width="110">已开考人数</th>
            <th width="110">状态</th>
            <th width="280" class="text-end">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="session in sessions" :key="session.id">
            <td class="text-secondary small">#{{ session.id }}</td>
            <td class="fw-bold">{{ session.examTitle }}</td>
            <td>{{ formatDateTime(session.startTime) }}</td>
            <td>{{ session.durationMinutes }} 分钟</td>
            <td><span class="fw-bold">{{ session.bookedCount }}</span></td>
            <td>{{ session.capacity }}</td>
            <td>
              <span class="badge bg-primary-soft text-primary">{{ session.startedCount ?? 0 }}</span>
            </td>
            <td>
              <span class="badge" :class="statusBadgeClass[session.status]">
                {{ statusLabels[session.status] || session.status }}
              </span>
            </td>
            <td class="text-end">
              <div class="d-inline-flex gap-2">
                <button class="btn btn-sm btn-light text-primary px-3" @click="viewReservations(session)">
                  预约名单
                </button>
                <button class="btn btn-sm btn-light text-primary px-3" @click="openEdit(session)">编辑</button>
                <button
                  v-if="session.status !== 'CLOSED'"
                  class="btn btn-sm btn-light text-danger px-3"
                  :disabled="offliningIds.has(session.id)"
                  @click="offlineSession(session)"
                >
                  <span v-if="offliningIds.has(session.id)">处理中</span>
                  <span v-else>下线</span>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showSessionModal" class="modal-backdrop-apple fade show" @click="closeModal"></div>
    <div v-if="showSessionModal" class="modal-apple fade show d-block" tabindex="-1">
      <div class="modal-dialog">
        <div class="apple-modal-content border-0 p-3">
          <div class="apple-modal-header">
            <h5 class="modal-title fw-bold">{{ modalTitle }}</h5>
          </div>
          <div class="apple-modal-body py-4">
            <div class="mb-3">
              <label class="form-label">关联考试</label>
              <select v-model="form.examId" class="form-select rounded-4" :disabled="!!editingId">
                <option v-for="exam in exams" :key="exam.id" :value="exam.id">{{ exam.title }}</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label">开始时间</label>
              <input type="datetime-local" v-model="form.startTime" class="form-control rounded-4">
            </div>
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label">考试时长（小时，支持小数，如 1.5）</label>
                <input type="number" min="0.1" step="0.1" v-model.number="form.durationHours" class="form-control rounded-4">
                <small class="text-secondary d-block mt-1">
                  系统将按「分钟」整数保存（{{ hoursToMinutes(form.durationHours) || 0 }} 分钟）
                </small>
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label">最大预约人数</label>
                <input type="number" min="1" v-model.number="form.capacity" class="form-control rounded-4">
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label">迟到入场宽限（分钟）</label>
              <input type="number" min="0" step="1" v-model.number="form.lateGraceMinutes" class="form-control rounded-4">
              <small class="text-secondary d-block mt-1">场次开始后，超过该宽限时间仍未进入的用户将无法入场。</small>
            </div>
            <div class="mb-3">
              <label class="form-label">切屏强制交卷阈值（次）</label>
              <input type="number" min="1" step="1" v-model.number="form.screenSwitchLimit" class="form-control rounded-4">
              <small class="text-secondary d-block mt-1">作答期间累计切屏达到该次数将被强制交卷。</small>
            </div>
            <div class="mb-3">
              <label class="form-label">场次状态</label>
              <select v-model="form.status" class="form-select rounded-4">
                <option value="DRAFT">草稿</option>
                <option value="OPEN">开放预约</option>
                <option value="CLOSED">已下线</option>
                <option value="FINISHED">已结束</option>
              </select>
            </div>
            <div class="small text-secondary">
              <i class="bi bi-info-circle me-1"></i>
              用户只能预约距离开始时间不少于 30 分钟的场次。
            </div>
          </div>
          <div class="apple-modal-footer">
            <button type="button" class="btn btn-secondary flex-grow-1 py-3" @click="closeModal">取消</button>
            <button
              type="button"
              class="btn btn-primary flex-grow-1 py-3 shadow-sm"
              :disabled="saving"
              @click="saveSession"
            >
              <span v-if="saving">保存中...</span>
              <span v-else>保存场次</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showReservationsModal" class="modal-backdrop-apple fade show" @click="showReservationsModal = false"></div>
    <div v-if="showReservationsModal" class="modal-apple fade show d-block" tabindex="-1">
      <div class="modal-dialog modal-dialog-lg">
        <div class="apple-modal-content border-0 p-3">
          <div class="apple-modal-header d-flex justify-content-between align-items-center">
            <h5 class="modal-title fw-bold">预约名单</h5>
            <button type="button" class="btn-close" @click="showReservationsModal = false"></button>
          </div>
          <div class="apple-modal-body py-4">
            <div v-if="reservationsLoading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status"></div>
            </div>
            <div v-else-if="reservations.length === 0" class="text-center py-5 text-secondary">
              暂无预约用户
            </div>
            <div v-else class="table-responsive">
              <table class="table align-middle">
                <thead>
                  <tr>
                    <th width="80">ID</th>
                    <th>用户名</th>
                    <th>预约时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in reservations" :key="item.id">
                    <td class="text-secondary small">#{{ item.id }}</td>
                    <td class="fw-bold">{{ item.username }}</td>
                    <td>{{ formatDateTime(item.reservedAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="apple-modal-footer">
            <button type="button" class="btn btn-secondary flex-grow-1 py-3" @click="showReservationsModal = false">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bg-primary-soft {
  background-color: rgba(0, 122, 255, 0.1);
  color: var(--apple-blue);
}

.bg-success-soft {
  background-color: rgba(52, 199, 89, 0.1);
  color: #1a7d32;
}

.bg-danger-soft {
  background-color: rgba(255, 59, 48, 0.1);
  color: #d32f2f;
}
</style>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getSessionDashboard } from '../api/adminSessions'
import { useToast } from '../composables/useToast'

const toast = useToast()

const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')

const statusLabels = {
  DRAFT: '草稿',
  OPEN: '开放预约',
  CLOSED: '已下线',
  FINISHED: '已结束'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const fetchDashboard = async () => {
  loading.value = true
  try {
    const res = await getSessionDashboard(statusFilter.value || undefined)
    rows.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch dashboard:', error)
    toast.error('加载场次看板失败')
  } finally {
    loading.value = false
  }
}

const escapeCsv = (value) => {
  if (value === null || value === undefined) return ''
  const str = String(value)
  if (/[",\n]/.test(str)) {
    return '"' + str.replace(/"/g, '""') + '"'
  }
  return str
}

const exportCsv = () => {
  if (rows.value.length === 0) {
    toast.warning('当前没有可导出的数据')
    return
  }
  const headers = [
    '场次ID', '考试名称', '开始时间', '时长(分钟)', '状态', '容量',
    '预约人数', '实际开考人数', '按时完成人数', '超时自动交卷人数',
    '切屏强制交卷人数', '平均用时(分钟)'
  ]
  const lines = [headers.join(',')]
  rows.value.forEach((r) => {
    lines.push([
      r.sessionId,
      r.examTitle,
      formatDateTime(r.sessionStartTime),
      r.durationMinutes,
      statusLabels[r.status] || r.status || '',
      r.capacity,
      r.reservedCount,
      r.startedCount,
      r.completedOnTimeCount,
      r.timeoutSubmissionCount,
      r.screenSwitchForcedCount,
      r.averageDurationMinutes
    ].map(escapeCsv).join(','))
  })

  const csv = '\uFEFF' + lines.join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const datePart = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`
  const first = rows.value[0]
  const examPart = first?.examTitle ? `_${first.examTitle}` : ''
  link.href = url
  link.download = `场次数据看板${examPart}_${datePart}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  toast.success('CSV 已导出')
}

watch(statusFilter, () => {
  fetchDashboard()
})

onMounted(() => {
  fetchDashboard()
})
</script>

<template>
  <div class="session-dashboard">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
      <h3 class="fw-bold h4 mb-0">场次数据看板</h3>
      <div class="d-flex align-items-center gap-2">
        <label class="small text-secondary mb-0">状态筛选</label>
        <select v-model="statusFilter" class="form-select form-select-sm" style="width: 140px;">
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="OPEN">开放预约</option>
          <option value="CLOSED">已下线</option>
          <option value="FINISHED">已结束</option>
        </select>
        <button class="btn btn-outline-primary btn-sm px-3" @click="exportCsv">
          <i class="bi bi-download me-1"></i>导出 CSV
        </button>
        <button class="btn btn-secondary btn-sm px-3" @click="fetchDashboard">
          <i class="bi bi-arrow-clockwise me-1"></i>刷新
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
      <p class="text-secondary mt-3">正在加载看板数据...</p>
    </div>

    <div v-else-if="rows.length === 0" class="text-center py-5">
      <i class="bi bi-bar-chart-line display-1 d-block text-secondary opacity-25 mb-3"></i>
      <h4 class="fw-bold mb-2">暂无看板数据</h4>
      <p class="text-secondary mb-0">当前筛选条件下没有场次。</p>
    </div>

    <div v-else class="table-responsive">
      <table class="table table-hover align-middle">
        <thead>
          <tr>
            <th>考试 / 场次</th>
            <th>开始时间</th>
            <th width="90">预约人数</th>
            <th width="100">实际开考</th>
            <th width="100">按时完成</th>
            <th width="120">超时交卷</th>
            <th width="120">切屏强制交卷</th>
            <th width="120">平均用时</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.sessionId">
            <td>
              <div class="fw-bold">{{ row.examTitle }}</div>
              <div class="small text-secondary">
                #{{ row.sessionId }} · {{ row.durationMinutes }} 分钟 ·
                <span class="badge bg-light text-secondary">{{ statusLabels[row.status] || row.status }}</span>
              </div>
            </td>
            <td>{{ formatDateTime(row.sessionStartTime) }}</td>
            <td><span class="fw-bold">{{ row.reservedCount }}</span> / {{ row.capacity }}</td>
            <td>{{ row.startedCount }}</td>
            <td>
              <span class="badge bg-success-soft text-success">{{ row.completedOnTimeCount }}</span>
            </td>
            <td>
              <span class="badge" :class="row.timeoutSubmissionCount > 0 ? 'bg-warning text-dark' : 'bg-light text-secondary'">
                {{ row.timeoutSubmissionCount }}
              </span>
            </td>
            <td>
              <span class="badge" :class="row.screenSwitchForcedCount > 0 ? 'bg-danger-soft text-danger' : 'bg-light text-secondary'">
                {{ row.screenSwitchForcedCount }}
              </span>
            </td>
            <td>{{ row.averageDurationMinutes }} 分钟</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.bg-success-soft {
  background-color: rgba(52, 199, 89, 0.1);
  color: #1a7d32;
}
.bg-danger-soft {
  background-color: rgba(255, 59, 48, 0.1);
  color: #d32f2f;
}
</style>

<template>
  <div class="take-exam container py-2">
    <div v-if="loading" class="text-center py-5 fade-in">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
      <p class="mt-3 text-secondary">正在为您准备试卷...</p>
    </div>

    <!-- 开考准入失败：按错误码展示不同文案 -->
    <div v-else-if="accessError" class="row justify-content-center fade-in-up">
      <div class="col-md-8 col-lg-6">
        <div class="glass-panel text-center py-5 px-4" style="border-radius: 32px;">
          <div class="mb-4">
            <i class="bi display-1" :class="accessError.code === 'RSV_SESSION_NOT_STARTED' ? 'bi-hourglass-top text-primary' : 'bi-shield-lock text-danger'"></i>
          </div>
          <h2 class="fw-bold mb-3 h3">暂时无法进入考试</h2>
          <p class="text-secondary fs-5 mb-5">{{ accessError.message }}</p>
          <div class="d-flex flex-wrap justify-content-center gap-3">
            <router-link to="/sessions" class="btn btn-primary px-5 py-3 rounded-4 shadow-sm">
              <span>前往场次预约</span>
            </router-link>
            <router-link to="/" class="btn btn-secondary px-5 py-3 rounded-4">
              <span>回到首页</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="result" class="row justify-content-center fade-in-up">
      <div class="col-md-10 col-lg-9">
        <div class="glass-panel text-center py-5 px-4" style="border-radius: 32px;">
          <div class="mb-5">
            <div class="score-circle mx-auto mb-4 d-flex flex-column justify-content-center align-items-center">
              <span class="score-number">{{ result.score }}</span>
              <span class="score-total">/ {{ exam.totalScore }}</span>
            </div>
            <h2 class="fw-bold mb-2">考试已完成</h2>
            <p class="text-secondary mb-0">系统已成功记录您的本次考试成绩</p>
            <p v-if="result.finishType === 'FORCED'" class="text-danger fw-bold mt-2 mb-0">
              <i class="bi bi-exclamation-triangle-fill me-1"></i>因切屏次数达到上限，本次考试被强制交卷
            </p>
            <p v-else-if="result.finishType === 'TIMEOUT'" class="text-warning fw-bold mt-2 mb-0">
              <i class="bi bi-clock-history me-1"></i>作答时间已到，本次考试由系统自动交卷
            </p>
          </div>
          
          <div class="row g-4 mb-5 text-start px-md-5">
            <div class="col-6 col-md-4">
              <div class="p-3 rounded-4 bg-light">
                <div class="small text-secondary mb-1">考生</div>
                <div class="fw-bold">{{ authStore.user.username }}</div>
              </div>
            </div>
            <div class="col-6 col-md-4">
              <div class="p-3 rounded-4 bg-light">
                <div class="small text-secondary mb-1">考试时长</div>
                <div class="fw-bold">{{ exam.duration }} 分钟</div>
              </div>
            </div>
            <div class="col-6 col-md-4" v-if="result.usedMinutes !== null && result.usedMinutes !== undefined">
              <div class="p-3 rounded-4 bg-light">
                <div class="small text-secondary mb-1">作答用时</div>
                <div class="fw-bold">{{ result.usedMinutes }} 分钟</div>
              </div>
            </div>
            <div class="col-12 col-md-4">
              <div class="p-3 rounded-4 bg-light">
                <div class="small text-secondary mb-1">通过状态</div>
                <div class="fw-bold" :class="result.score >= exam.totalScore * 0.6 ? 'text-success' : 'text-danger'">
                  {{ result.score >= exam.totalScore * 0.6 ? '已通过' : '未通过' }}
                </div>
              </div>
            </div>
          </div>
          
          <div class="d-flex flex-wrap justify-content-center gap-3">
            <button @click="showReview = !showReview" class="btn btn-outline-primary btn-lg px-4 px-md-5 py-3 shadow-sm rounded-4">
              <span>{{ showReview ? '隐藏解析' : '查看解析' }}</span>
              <i class="bi ms-2" :class="showReview ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
            </button>
            <router-link to="/exams" class="btn btn-primary btn-lg px-4 px-md-5 py-3 shadow-sm rounded-4">
              <span>返回列表</span>
            </router-link>
            <router-link to="/" class="btn btn-secondary btn-lg px-4 px-md-5 py-3 rounded-4">
              <span>回到首页</span>
            </router-link>
          </div>
        </div>

        <!-- Detailed Review Section -->
        <div v-if="showReview" class="mt-5 fade-in">
          <h3 class="fw-bold mb-4 text-center">答题详情与解析</h3>
          <div v-for="(eq, index) in exam.questions" :key="eq.id" class="card mb-4 border-0 shadow-sm rounded-4 overflow-hidden">
            <div class="card-header border-0 py-3 px-4 d-flex justify-content-between align-items-center" 
                 :class="answers[eq.question.id]?.trim().toLowerCase() === eq.question.answer?.trim().toLowerCase() ? 'bg-success-soft' : 'bg-danger-soft'">
              <span class="fw-bold">第 {{ index + 1 }} 题</span>
              <span class="badge" :class="answers[eq.question.id]?.trim().toLowerCase() === eq.question.answer?.trim().toLowerCase() ? 'bg-success' : 'bg-danger'">
                {{ answers[eq.question.id]?.trim().toLowerCase() === eq.question.answer?.trim().toLowerCase() ? '回答正确' : '回答错误' }}
              </span>
            </div>
            <div class="card-body p-4">
              <h5 class="fw-bold mb-3">{{ eq.question.content }}</h5>
              <div class="mb-3">
                <div class="small text-secondary mb-1">您的答案:</div>
                <div class="p-2 bg-light rounded-3 fw-bold">{{ answers[eq.question.id] || '(未回答)' }}</div>
              </div>
              <div class="mb-3">
                <div class="small text-secondary mb-1">正确答案:</div>
                <div class="p-2 bg-success-soft text-success rounded-3 fw-bold">{{ formatAnswer(eq.question.answer, eq.question.type) }}</div>
              </div>
              <div v-if="eq.question.analysis" class="mt-4 p-3 rounded-3 border-start border-4 border-primary bg-light">
                <div class="fw-bold text-primary mb-1 small"><i class="bi bi-lightbulb-fill me-1"></i>答案解析</div>
                <p class="mb-0 small text-secondary lh-lg">{{ eq.question.analysis }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="exam">
      <!-- Sticky Header for Timer -->
      <div class="sticky-top glass-nav py-3 mb-5" style="z-index: 1020; top: 0; margin-top: 0; margin-left: -1rem; margin-right: -1rem;">
        <div class="container d-flex justify-content-between align-items-center">
          <div class="d-flex align-items-center gap-3">
            <button class="btn btn-secondary btn-sm p-2 rounded-3" @click="$router.push('/exams')">
              <i class="bi bi-chevron-left"></i>
            </button>
            <h4 class="m-0 fw-bold text-truncate d-none d-sm-block" style="max-width: 400px;">{{ exam.title }}</h4>
          </div>
          <div class="d-flex align-items-center gap-4">
            <div class="text-end" v-if="sessionInfo">
              <div class="small text-secondary fw-600">切屏次数</div>
              <div class="fw-bold fs-5 tabular-nums" :class="{ 'text-danger animate-pulse': switchCount > 0 }">
                {{ switchCount }}/{{ sessionInfo.maxSwitchCount }}
              </div>
            </div>
            <div class="text-end">
              <div class="small text-secondary fw-600">剩余时间</div>
              <div class="fw-bold fs-5 tabular-nums" :class="{'text-danger animate-pulse': timeLeft < 300}">
                {{ formatTime(timeLeft) }}
              </div>
            </div>
            <button class="btn btn-primary px-4 py-2" @click="confirmSubmit">交卷</button>
          </div>
        </div>
      </div>

      <div class="row">
        <div class="col-lg-9 mx-auto">
          <div class="glass-panel mb-5 p-4" style="border-radius: 20px;">
            <p class="text-secondary mb-3">{{ exam.description }}</p>
            <div class="d-flex flex-wrap gap-4 text-secondary small fw-600">
              <span><i class="bi bi-clock-history me-2"></i>限时: {{ exam.duration }} 分钟</span>
              <span><i class="bi bi-list-check me-2"></i>题目: {{ exam.questions.length }} 题</span>
              <span><i class="bi bi-award me-2"></i>总分: {{ exam.totalScore }} 分</span>
            </div>
          </div>

          <form @submit.prevent="confirmSubmit" class="fade-in-up">
            <div v-for="(examQuestion, index) in exam.questions" :key="examQuestion.id" class="card mb-4 border-0 shadow-sm overflow-visible">
              <div class="card-header bg-transparent border-0 pt-4 pb-0 px-4">
                <div class="d-flex justify-content-between align-items-center">
                  <span class="badge bg-primary-soft text-primary px-3 py-2">{{ getQuestionTypeLabel(examQuestion.question.type) }}</span>
                  <span class="text-secondary small fw-600">本题 {{ examQuestion.score }} 分</span>
                </div>
              </div>
              <div class="card-body p-4">
                <h5 class="fw-bold mb-4 lh-base">
                  <span class="text-primary me-2">{{ index + 1 }}.</span>
                  {{ examQuestion.question.content }}
                </h5>
                
                <!-- Single Choice -->
                <div v-if="examQuestion.question.type === 'SINGLE_CHOICE'" class="d-flex flex-column gap-3">
                  <div v-for="(option, optIndex) in parseOptions(examQuestion.question.options)" :key="optIndex">
                    <input type="radio" class="btn-check" 
                           :name="'q-' + examQuestion.question.id" 
                           :id="'q-' + examQuestion.question.id + '-' + optIndex"
                           :value="getOptionLabel(optIndex)"
                           v-model="answers[examQuestion.question.id]">
                    <label class="btn btn-outline-secondary text-start w-100 p-3 rounded-4 option-btn" :for="'q-' + examQuestion.question.id + '-' + optIndex">
                      <span class="option-label me-3">{{ getOptionLabel(optIndex) }}</span>
                      <span class="option-text">{{ option }}</span>
                    </label>
                  </div>
                </div>

                <!-- True/False -->
                <div v-else-if="examQuestion.question.type === 'TRUE_FALSE'" class="d-flex gap-3">
                  <div class="flex-fill">
                    <input type="radio" class="btn-check" 
                           :name="'q-' + examQuestion.question.id" 
                           :id="'q-' + examQuestion.question.id + '-T'"
                           value="T"
                           v-model="answers[examQuestion.question.id]">
                    <label class="btn btn-outline-secondary w-100 p-4 rounded-4 option-btn text-center" :for="'q-' + examQuestion.question.id + '-T'">
                      <i class="bi bi-check-lg display-6 d-block mb-2"></i>
                      <span>正确</span>
                    </label>
                  </div>
                  <div class="flex-fill">
                    <input type="radio" class="btn-check" 
                           :name="'q-' + examQuestion.question.id" 
                           :id="'q-' + examQuestion.question.id + '-F'"
                           value="F"
                           v-model="answers[examQuestion.question.id]">
                    <label class="btn btn-outline-secondary w-100 p-4 rounded-4 option-btn text-center" :for="'q-' + examQuestion.question.id + '-F'">
                      <i class="bi bi-x-lg display-6 d-block mb-2"></i>
                      <span>错误</span>
                    </label>
                  </div>
                </div>

                 <!-- Multi Choice -->
                 <div v-else-if="examQuestion.question.type === 'MULTI_CHOICE'" class="d-flex flex-column gap-3">
                   <div v-for="(option, optIndex) in parseOptions(examQuestion.question.options)" :key="optIndex">
                    <input type="checkbox" class="btn-check" 
                           :id="'q-' + examQuestion.question.id + '-' + optIndex"
                           :value="getOptionLabel(optIndex)"
                           v-model="multiChoiceAnswers[examQuestion.question.id]"
                           @change="updateMultiChoice(examQuestion.question.id)">
                    <label class="btn btn-outline-secondary text-start w-100 p-3 rounded-4 option-btn" :for="'q-' + examQuestion.question.id + '-' + optIndex">
                      <span class="option-label me-3">{{ getOptionLabel(optIndex) }}</span>
                      <span class="option-text">{{ option }}</span>
                    </label>
                  </div>
                </div>

                <!-- Fill in Blank -->
                <div v-else-if="examQuestion.question.type === 'FILL_IN_BLANK'">
                   <input type="text" class="form-control form-control-lg py-3 rounded-4" 
                          placeholder="请输入您的答案..."
                          v-model="answers[examQuestion.question.id]">
                </div>

                <!-- Short Answer -->
                <div v-else>
                   <textarea class="form-control form-control-lg rounded-4" rows="5"
                             placeholder="请在此输入您的详细回答..."
                             v-model="answers[examQuestion.question.id]"></textarea>
                </div>
              </div>
            </div>

            <div class="d-grid mt-5 mb-5">
              <button type="submit" class="btn btn-primary btn-lg py-3 shadow-lg rounded-4">
                <span>提交试卷</span>
                <i class="bi bi-send-fill ms-2"></i>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Submit Confirmation Modal -->
    <div v-if="showSubmitModal" class="modal-backdrop-apple fade show" @click="showSubmitModal = false"></div>
    <div v-if="showSubmitModal" class="modal-apple fade show d-block" tabindex="-1">
      <div class="modal-dialog">
        <div class="apple-modal-content border-0 p-3">
          <div class="apple-modal-header">
            <h5 class="modal-title fw-bold">确认提交试卷？</h5>
          </div>
          <div class="apple-modal-body py-4">
            <p class="text-secondary mb-4">一旦提交，您将无法再次修改答案。建议您在提交前仔细检查所有题目。</p>
            <div v-if="timeLeft > 0" class="p-3 bg-light rounded-4 d-flex align-items-center gap-3 border">
              <i class="bi bi-clock-history text-primary fs-4"></i>
              <div>
                <div class="small text-secondary">当前剩余时间</div>
                <div class="fw-bold tabular-nums">{{ formatTime(timeLeft) }}</div>
              </div>
            </div>
          </div>
          <div class="apple-modal-footer">
            <button type="button" class="btn btn-secondary flex-grow-1 py-3" @click="showSubmitModal = false">继续检查</button>
            <button type="button" class="btn btn-primary flex-grow-1 py-3 shadow-sm" @click="submitExam(true)">确认交卷</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.take-exam {
  max-width: 1000px;
}

.bg-primary-soft {
  background-color: rgba(0, 122, 255, 0.1);
}

.fw-600 {
  font-weight: 600;
}

.tabular-nums {
  font-variant-numeric: tabular-nums;
}

.animate-pulse {
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.6; }
  100% { opacity: 1; }
}

.option-btn {
  border: 2px solid var(--apple-gray-5);
  background-color: white;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.option-btn:hover {
  background-color: var(--apple-gray-6);
  border-color: var(--apple-gray-4);
  transform: translateY(-2px);
}

.btn-check:checked + .option-btn {
  background-color: rgba(0, 122, 255, 0.05);
  border-color: var(--apple-blue);
  color: var(--apple-blue);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.1);
}

.option-label {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  background-color: var(--apple-gray-6);
  border-radius: 8px;
  font-weight: 700;
  color: var(--text-secondary);
}

.btn-check:checked + .option-btn .option-label {
  background-color: var(--apple-blue);
  color: white;
}

/* Score Circle */
.score-circle {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--apple-blue), var(--apple-cyan));
  color: white;
  box-shadow: 0 12px 32px rgba(0, 122, 255, 0.3);
}

.score-number {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1;
}

.score-total {
  font-size: 1.1rem;
  opacity: 0.8;
  font-weight: 600;
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

<script setup>
import { ref, onMounted, reactive, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { examApi } from '../api/exams'
import { sessionApi } from '../api/sessions'
import { reservationApi } from '../api/reservations'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()
const exam = ref(null)
const loading = ref(true)
const answers = reactive({})
const multiChoiceAnswers = reactive({})
const result = ref(null)
const showReview = ref(false)

// 开考准入状态
const sessionInfo = ref(null)
const accessError = ref(null)

// 防切屏监考状态（累计次数以后端落库为准）
const switchCount = ref(0)

// A5: 按后端 RSV_ 业务错误码分别展示准入失败文案
const ACCESS_ERROR_MESSAGES = {
  RSV_NOT_RESERVED: '您尚未预约该场次，请先在「场次预约」页完成预约',
  RSV_RESERVATION_CANCELLED: '您的预约已取消，无法进入本次考试',
  RSV_SESSION_NOT_STARTED: '该场次尚未开始，请到达开始时间后再进入',
  RSV_SESSION_ENDED: '该场次已结束，无法进入考试',
  RSV_SESSION_NOT_FOUND: '场次不存在或已下线',
  RSV_SUBMIT_TIMEOUT: '已超出作答窗口，本次作答已按超时交卷处理',
  RSV_ALREADY_SUBMITTED: '本场次已交卷，无法继续作答'
}

const rsvErrorMessage = (error, fallback) => {
  const data = error?.response?.data
  if (data?.errorCode && ACCESS_ERROR_MESSAGES[data.errorCode]) {
    return ACCESS_ERROR_MESSAGES[data.errorCode]
  }
  return data?.message || fallback
}

// Timer state
const timeLeft = ref(0)
let timerInterval = null

const parseOptions = (options) => {
  if (!options) return []
  if (Array.isArray(options)) return options
  try {
    return JSON.parse(options)
  } catch (e) {
    return []
  }
}

const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index) // A, B, C...
}

const formatAnswer = (answer, type) => {
  if (type === 'SHORT_ANSWER') {
    try {
      const parsed = JSON.parse(answer)
      return parsed.standard || answer
    } catch (e) {
      return answer
    }
  }
  return answer
}

const getQuestionTypeLabel = (type) => {
  const map = {
    'SINGLE_CHOICE': '单选题',
    'MULTI_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'FILL_IN_BLANK': '填空题',
    'SHORT_ANSWER': '简答题'
  }
  return map[type] || '题目'
}

const updateMultiChoice = (questionId) => {
  if (!multiChoiceAnswers[questionId]) {
    multiChoiceAnswers[questionId] = []
  }
  // Sort and join
  answers[questionId] = multiChoiceAnswers[questionId].sort().join(',')
}

const formatTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  
  if (h > 0) {
    return `${h}:${pad(m)}:${pad(s)}`
  }
  return `${pad(m)}:${pad(s)}`
}

const pad = (num) => num.toString().padStart(2, '0')

const startTimer = () => {
  if (!exam.value) return
  if (sessionInfo.value?.endTime) {
    // 以服务端下发的作答窗口截止时间为准，本地每秒重算剩余秒数
    const endTs = new Date(sessionInfo.value.endTime).getTime()
    timeLeft.value = Math.max(0, Math.floor((endTs - Date.now()) / 1000))
  } else {
    timeLeft.value = exam.value.duration * 60 // convert to seconds
  }

  timerInterval = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      clearInterval(timerInterval)
      toast.warning('考试时间到，系统将自动提交试卷！', 5000)
      submitExam(true) // force submit
    }
  }, 1000)
}

const fetchExam = async () => {
  // 开考准入：必须携带 sessionId 且通过服务端准入校验
  const sessionId = route.query.sessionId ? parseInt(route.query.sessionId) : null
  if (!sessionId) {
    accessError.value = { code: 'RSV_NOT_RESERVED', message: ACCESS_ERROR_MESSAGES.RSV_NOT_RESERVED }
    loading.value = false
    return
  }
  try {
    const accessRes = await sessionApi.checkAccess(sessionId)
    sessionInfo.value = accessRes.data.data
  } catch (error) {
    console.error('Access denied:', error)
    accessError.value = {
      code: error.response?.data?.errorCode || '',
      message: rsvErrorMessage(error, '无法进入考试')
    }
    loading.value = false
    return
  }

  try {
    const response = await examApi.get(route.params.id)
    exam.value = response.data.data
    // Initialize multiChoice arrays
    if (exam.value.questions) {
      exam.value.questions.forEach(eq => {
        if (eq.question.type === 'MULTI_CHOICE') {
          multiChoiceAnswers[eq.question.id] = []
        }
      })
    }
    startTimer()
  } catch (error) {
    console.error('Failed to fetch exam:', error)
    toast.error('加载考试失败')
  } finally {
    loading.value = false
  }
}

const showSubmitModal = ref(false)

// 切屏监听：页面不可见即上报后端累计，达上限由后端判定并强制交卷
const handleVisibilityChange = async () => {
  if (!document.hidden) return
  if (!exam.value || result.value || !sessionInfo.value?.reservationId) return
  try {
    const res = await reservationApi.reportSwitch(sessionInfo.value.reservationId)
    const data = res.data.data
    switchCount.value = data.switchCount
    if (data.forceSubmit) {
      toast.error(`切屏次数已达上限（${data.maxSwitchCount} 次），系统将强制交卷！`, 5000)
      submitExam(true)
    } else {
      toast.warning(`警告：您已切屏 ${data.switchCount}/${data.maxSwitchCount} 次，达到上限将被强制交卷！`, 4000)
    }
  } catch (error) {
    console.error('Failed to report screen switch:', error)
  }
}

const confirmSubmit = () => {
  showSubmitModal.value = true
}

const submitExam = async (force = false) => {
  if (!force) {
    showSubmitModal.value = true
    return
  }
  
  showSubmitModal.value = false

  if (timerInterval) clearInterval(timerInterval)

  try {
    const response = await examApi.submit(route.params.id, {
      examId: parseInt(route.params.id),
      sessionId: sessionInfo.value?.sessionId ?? null,
      reservationId: sessionInfo.value?.reservationId ?? null,
      answers: answers
    })
    result.value = response.data.data
    window.scrollTo({ top: 0, behavior: 'smooth' })
    toast.success('考试已提交！')
  } catch (error) {
    console.error('Failed to submit exam:', error)
    if (error.response?.data?.errorCode === 'RSV_SUBMIT_TIMEOUT') {
      // 服务端已按超时落库本次作答
      toast.error(ACCESS_ERROR_MESSAGES.RSV_SUBMIT_TIMEOUT)
    } else {
      toast.error('提交失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  fetchExam()
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<style scoped>
.take-exam {
  max-width: 900px;
}
.sticky-top {
  box-shadow: var(--shadow-sm);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}
</style>
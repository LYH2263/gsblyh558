<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { listWrongQuestions } from '../api/wrongQuestions'
import { submitPractice } from '../api/practice'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const wrongRecords = ref([])
const currentRecordIndex = ref(0)
const userAnswer = ref('')
const multiChoiceAnswers = ref([])
const result = ref(null)
const resultAlert = ref(null)

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

const fetchWrongQuestions = async () => {
  loading.value = true
  try {
    const res = await listWrongQuestions()
    wrongRecords.value = res.data
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  const record = wrongRecords.value[currentRecordIndex.value]
  const question = record.question
  
  if (question.type === 'MULTI_CHOICE') {
    userAnswer.value = multiChoiceAnswers.value.sort().join(',')
  }
  
  const res = await submitPractice({
    questionId: question.id,
    userAnswer: userAnswer.value
  })
  
  const isCorrect = res.data
  result.value = isCorrect ? '回答正确! 已从错题本移除。' : '回答错误。正确答案: ' + formatAnswer(question.answer, question.type)
  
  // Wait for result to render and scroll it into view
  nextTick(() => {
    if (resultAlert.value) {
      resultAlert.value.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  })
}

const nextQuestion = () => {
  // Capture current scroll position
  const currentScroll = window.scrollY
  
  if (currentRecordIndex.value < wrongRecords.value.length - 1) {
    currentRecordIndex.value++
    userAnswer.value = ''
    multiChoiceAnswers.value = []
    result.value = null
  } else {
    // Refresh to clear removed ones
    fetchWrongQuestions()
    currentRecordIndex.value = 0
    userAnswer.value = ''
    multiChoiceAnswers.value = []
    result.value = null
  }
  
  // Ensure the scroll position is maintained after the content potentially changes height
  nextTick(() => {
    window.scrollTo(0, currentScroll)
  })
}

const getOptionClass = (optionLabel) => {
  if (!result.value) return ''
  const question = wrongRecords.value[currentRecordIndex.value].question
  const correctAnswers = question.answer.split(',')
  const userAnswers = userAnswer.value.split(',')
  
  if (correctAnswers.includes(optionLabel)) {
    return 'option-correct'
  }
  if (userAnswers.includes(optionLabel) && !correctAnswers.includes(optionLabel)) {
    return 'option-wrong'
  }
  return ''
}

onMounted(fetchWrongQuestions)
</script>

<template>
  <div class="wrong-questions position-relative py-2">
    <div v-if="loading" class="loading-overlay glass-panel">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div class="d-flex justify-content-between align-items-end mb-5 fade-in">
      <div>
        <h1 class="apple-title mb-1">错题本</h1>
        <p class="text-secondary mb-0">回顾并重新练习您之前回答错误的题目</p>
      </div>
    </div>

    <div v-if="wrongRecords.length === 0" class="text-center py-5 fade-in">
      <div class="glass-panel d-inline-block p-5 rounded-5">
        <i class="bi bi-check-circle-fill display-1 text-success mb-4 d-block"></i>
        <h3 class="fw-bold mb-2">太棒了!</h3>
        <p class="text-secondary fs-5 mb-0">您的错题本空空如也，继续保持！</p>
      </div>
    </div>

    <div v-else class="row justify-content-center">
      <div class="col-lg-8">
        <div class="glass-panel p-4 p-md-5 fade-in-up" style="border-radius: 28px;">
          <div class="d-flex justify-content-between align-items-center mb-4">
            <span class="badge bg-primary-soft text-primary px-3 py-2">
              错题 {{ currentRecordIndex + 1 }} / {{ wrongRecords.length }}
            </span>
            <div class="progress flex-grow-1 mx-4" style="height: 6px; border-radius: 3px; background-color: var(--apple-gray-6);">
              <div class="progress-bar bg-primary" role="progressbar" 
                   :style="{ width: ((currentRecordIndex + 1) / wrongRecords.length * 100) + '%', transition: 'width 0.3s ease' }"></div>
            </div>
          </div>

          <h4 class="fw-bold mb-4 lh-base">{{ wrongRecords[currentRecordIndex].question.content }}</h4>
          
          <div class="options-container mb-5">
            <!-- Single Choice -->
            <div v-if="wrongRecords[currentRecordIndex].question.type === 'SINGLE_CHOICE'" class="d-flex flex-column gap-3">
              <div v-for="(opt, index) in parseOptions(wrongRecords[currentRecordIndex].question.options)" :key="index">
                <input type="radio" class="btn-check" :value="getOptionLabel(index)" v-model="userAnswer" :id="'wopt'+index" :disabled="result && result.startsWith('回答正确')">
                <label class="btn btn-outline-secondary text-start w-100 p-3 rounded-4 option-btn" 
                       :for="'wopt'+index"
                       :class="getOptionClass(getOptionLabel(index))">
                  <span class="option-label me-3">{{ getOptionLabel(index) }}</span>
                  <span class="option-text">{{ opt }}</span>
                </label>
              </div>
            </div>

            <!-- Multi Choice -->
            <div v-else-if="wrongRecords[currentRecordIndex].question.type === 'MULTI_CHOICE'" class="d-flex flex-column gap-3">
              <div v-for="(opt, index) in parseOptions(wrongRecords[currentRecordIndex].question.options)" :key="index">
                <input type="checkbox" class="btn-check" :value="getOptionLabel(index)" v-model="multiChoiceAnswers" :id="'wmopt'+index" :disabled="result && result.startsWith('回答正确')">
                <label class="btn btn-outline-secondary text-start w-100 p-3 rounded-4 option-btn" 
                       :for="'wmopt'+index"
                       :class="getOptionClass(getOptionLabel(index))">
                  <span class="option-label me-3">{{ getOptionLabel(index) }}</span>
                  <span class="option-text">{{ opt }}</span>
                </label>
              </div>
            </div>

            <!-- True/False -->
            <div v-else-if="wrongRecords[currentRecordIndex].question.type === 'TRUE_FALSE'" class="d-flex gap-3">
              <div class="flex-fill">
                <input type="radio" class="btn-check" value="T" v-model="userAnswer" id="wtf-t" :disabled="result && result.startsWith('回答正确')">
                <label class="btn btn-outline-secondary w-100 p-4 rounded-4 option-btn text-center" 
                       for="wtf-t"
                       :class="getOptionClass('T')">
                  <i class="bi bi-check-lg display-6 d-block mb-2"></i>
                  <span>正确</span>
                </label>
              </div>
              <div class="flex-fill">
                <input type="radio" class="btn-check" value="F" v-model="userAnswer" id="wtf-f" :disabled="result && result.startsWith('回答正确')">
                <label class="btn btn-outline-secondary w-100 p-4 rounded-4 option-btn text-center" 
                       for="wtf-f"
                       :class="getOptionClass('F')">
                  <i class="bi bi-x-lg display-6 d-block mb-2"></i>
                  <span>错误</span>
                </label>
              </div>
            </div>
            
            <!-- Fill in Blank -->
            <div v-else-if="wrongRecords[currentRecordIndex].question.type === 'FILL_IN_BLANK'" class="mb-3">
              <input type="text" v-model="userAnswer" class="form-control form-control-lg py-3 rounded-4" placeholder="请输入您的答案" :disabled="result && result.startsWith('回答正确')">
            </div>

            <!-- Short Answer -->
            <div v-else-if="wrongRecords[currentRecordIndex].question.type === 'SHORT_ANSWER'" class="mb-3">
              <textarea v-model="userAnswer" class="form-control form-control-lg rounded-4" rows="4" placeholder="请在此输入您的回答..." :disabled="result && result.startsWith('回答正确')"></textarea>
            </div>
          </div>

          <div class="d-flex gap-3">
            <button @click="submitAnswer" class="btn btn-primary btn-lg flex-grow-1 py-3 shadow-sm" v-if="!result || !result.startsWith('回答正确')">
              <span>重新挑战</span>
            </button>
            
            <template v-if="result && result.startsWith('回答正确')">
              <button @click="nextQuestion" class="btn btn-secondary btn-lg flex-grow-1 py-3" v-if="wrongRecords.length > 1">
                <span>下一题</span>
                <i class="bi bi-arrow-right ms-2"></i>
              </button>
              <router-link to="/" class="btn btn-success btn-lg flex-grow-1 py-3 shadow-sm" v-else>
                <span>完成挑战，返回首页</span>
                <i class="bi bi-house-door ms-2"></i>
              </router-link>
            </template>

            <button @click="nextQuestion" class="btn btn-outline-secondary btn-lg flex-grow-1 py-3" v-if="(!result || !result.startsWith('回答正确')) && wrongRecords.length > 1">
              <span>跳过此题</span>
            </button>
          </div>
          
          <transition name="fade">
            <div v-if="result" 
                 ref="resultAlert"
                 class="mt-4 fade-in">
              <div :class="result.startsWith('回答正确') ? 'alert-success-apple' : 'alert-danger-apple'" 
                   class="p-4 rounded-4 d-flex align-items-start gap-3 mb-3">
                <i :class="result.startsWith('回答正确') ? 'bi bi-check-circle-fill fs-4' : 'bi bi-exclamation-circle-fill fs-4'"></i>
                <div>
                  <h6 class="fw-bold mb-1">{{ result.startsWith('回答正确') ? '纠正成功!' : '还是不对' }}</h6>
                  <p class="mb-0">{{ result }}</p>
                </div>
              </div>

              <!-- Analysis Section -->
              <div class="analysis-panel p-4 rounded-4 border bg-white shadow-sm" v-if="wrongRecords[currentRecordIndex].question.analysis">
                <div class="d-flex align-items-center gap-2 mb-2 text-primary">
                  <i class="bi bi-lightbulb-fill"></i>
                  <h6 class="fw-bold mb-0">题目解析</h6>
                </div>
                <p class="mb-0 text-secondary lh-lg small">{{ wrongRecords[currentRecordIndex].question.analysis }}</p>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bg-primary-soft {
  background-color: rgba(0, 122, 255, 0.1);
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

.alert-success-apple {
  background-color: rgba(52, 199, 89, 0.1);
  color: #1a7d32;
  border: none;
}

.alert-danger-apple {
  background-color: rgba(255, 59, 48, 0.1);
  color: #d32f2f;
  border: none;
}

.option-correct {
  background-color: rgba(52, 199, 89, 0.15) !important;
  border-color: var(--apple-green) !important;
  color: #1a7d32 !important;
}

.option-correct .option-label {
  background-color: var(--apple-green) !important;
  color: white !important;
}

.option-wrong {
  background-color: rgba(255, 59, 48, 0.15) !important;
  border-color: var(--apple-red) !important;
  color: #d32f2f !important;
}

.option-wrong .option-label {
  background-color: var(--apple-red) !important;
  color: white !important;
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

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>

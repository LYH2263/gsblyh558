<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { listCategories } from '../api/categories'
import { listQuestions } from '../api/questions'
import { submitPractice } from '../api/practice'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const categories = ref([])
const questions = ref([])
const selectedCategoryId = ref(null)
const practiceMode = ref('sequential')
const currentQuestionIndex = ref(0)
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

const fetchCategories = async () => {
  loading.value = true
  try {
    const res = await listCategories()
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

const startPractice = async () => {
  loading.value = true
  try {
    const sortField = practiceMode.value === 'random' ? 'random' : 'id'
    const res = await listQuestions({
      size: 100,
      categoryId: selectedCategoryId.value,
      sortField,
      sortDir: 'asc'
    })

    questions.value = res.data.content
    currentQuestionIndex.value = 0
    result.value = null
    userAnswer.value = ''
    multiChoiceAnswers.value = []
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  const question = questions.value[currentQuestionIndex.value]
  
  if (question.type === 'MULTI_CHOICE') {
    userAnswer.value = multiChoiceAnswers.value.sort().join(',')
  }

  const res = await submitPractice({
    questionId: question.id,
    userAnswer: userAnswer.value
  })
  result.value = res.data ? '回答正确!' : '回答错误! 正确答案: ' + formatAnswer(question.answer, question.type)
  
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
  
  currentQuestionIndex.value++
  userAnswer.value = ''
  multiChoiceAnswers.value = []
  result.value = null
  
  // Ensure the scroll position is maintained after the content potentially changes height
  nextTick(() => {
    window.scrollTo(0, currentScroll)
  })
}

const getOptionClass = (optionLabel) => {
  if (!result.value) return ''
  const question = questions.value[currentQuestionIndex.value]
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

onMounted(fetchCategories)
</script>

<template>
  <div class="practice position-relative py-2">
    <div v-if="loading" class="loading-overlay glass-panel">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div class="d-flex justify-content-between align-items-end mb-5 fade-in">
      <div>
        <h1 class="apple-title mb-1">练习模式</h1>
        <p class="text-secondary mb-0">通过专项练习，攻克薄弱环节</p>
      </div>
    </div>
    
    <div class="row justify-content-center">
      <div class="col-lg-8">
        <div class="glass-panel p-4 p-md-5 fade-in-up" style="border-radius: 28px;">
          <div v-if="!questions.length">
            <div class="mb-4">
              <label class="form-label">选择知识分类</label>
              <select v-model="selectedCategoryId" class="form-select form-select-lg">
                <option :value="null" disabled>请选择分类...</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>

            <div class="mb-5">
              <label class="form-label">练习模式</label>
              <div class="d-flex gap-3">
                <div class="flex-fill">
                  <input type="radio" class="btn-check" name="mode" id="modeSeq" value="sequential" v-model="practiceMode" autocomplete="off">
                  <label class="btn btn-outline-primary w-100 py-3 rounded-4" for="modeSeq">
                    <i class="bi bi-list-ol me-2"></i>顺序练习
                  </label>
                </div>
                <div class="flex-fill">
                  <input type="radio" class="btn-check" name="mode" id="modeRand" value="random" v-model="practiceMode" autocomplete="off">
                  <label class="btn btn-outline-primary w-100 py-3 rounded-4" for="modeRand">
                    <i class="bi bi-shuffle me-2"></i>随机练习
                  </label>
                </div>
              </div>
            </div>

            <button @click="startPractice" class="btn btn-primary btn-lg w-100 py-3 shadow-sm" :disabled="!selectedCategoryId">
              <span>开始练习</span>
              <i class="bi bi-play-fill ms-2"></i>
            </button>
          </div>

          <div v-else-if="currentQuestionIndex < questions.length" class="fade-in">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <span class="badge bg-primary-soft text-primary px-3 py-2">
                题目 {{ currentQuestionIndex + 1 }} / {{ questions.length }}
              </span>
              <div class="progress flex-grow-1 mx-4" style="height: 6px; border-radius: 3px;">
                <div class="progress-bar bg-primary" role="progressbar" 
                     :style="{ width: ((currentQuestionIndex + 1) / questions.length * 100) + '%' }"></div>
              </div>
            </div>

            <h4 class="fw-bold mb-4 lh-base">{{ questions[currentQuestionIndex].content }}</h4>
            
            <div class="options-container mb-5">
              <!-- Single Choice -->
              <div v-if="questions[currentQuestionIndex].type === 'SINGLE_CHOICE'" class="d-flex flex-column gap-3">
                <div v-for="(opt, index) in parseOptions(questions[currentQuestionIndex].options)" :key="index">
                  <input type="radio" class="btn-check" :value="getOptionLabel(index)" v-model="userAnswer" :id="'opt'+index" :disabled="result">
                  <label class="btn btn-outline-secondary text-start w-100 p-3 rounded-4 option-btn" 
                         :for="'opt'+index"
                         :class="getOptionClass(getOptionLabel(index))">
                    <span class="option-label me-3">{{ getOptionLabel(index) }}</span>
                    <span class="option-text">{{ opt }}</span>
                  </label>
                </div>
              </div>

              <!-- Multi Choice -->
              <div v-else-if="questions[currentQuestionIndex].type === 'MULTI_CHOICE'" class="d-flex flex-column gap-3">
                <div v-for="(opt, index) in parseOptions(questions[currentQuestionIndex].options)" :key="index">
                  <input type="checkbox" class="btn-check" :value="getOptionLabel(index)" v-model="multiChoiceAnswers" :id="'mopt'+index" :disabled="result">
                  <label class="btn btn-outline-secondary text-start w-100 p-3 rounded-4 option-btn" 
                         :for="'mopt'+index"
                         :class="getOptionClass(getOptionLabel(index))">
                    <span class="option-label me-3">{{ getOptionLabel(index) }}</span>
                    <span class="option-text">{{ opt }}</span>
                  </label>
                </div>
              </div>

              <!-- True/False -->
              <div v-else-if="questions[currentQuestionIndex].type === 'TRUE_FALSE'" class="d-flex gap-3">
                <div class="flex-fill">
                  <input type="radio" class="btn-check" value="T" v-model="userAnswer" id="tf-t" :disabled="result">
                  <label class="btn btn-outline-secondary w-100 p-4 rounded-4 option-btn" 
                         for="tf-t"
                         :class="getOptionClass('T')">
                    <i class="bi bi-check-lg display-6 d-block mb-2"></i>
                    <span>正确</span>
                  </label>
                </div>
                <div class="flex-fill">
                  <input type="radio" class="btn-check" value="F" v-model="userAnswer" id="tf-f" :disabled="result">
                  <label class="btn btn-outline-secondary w-100 p-4 rounded-4 option-btn" 
                         for="tf-f"
                         :class="getOptionClass('F')">
                    <i class="bi bi-x-lg display-6 d-block mb-2"></i>
                    <span>错误</span>
                  </label>
                </div>
              </div>
              
              <!-- Fill in Blank -->
              <div v-else-if="questions[currentQuestionIndex].type === 'FILL_IN_BLANK'" class="mb-3">
                <input type="text" v-model="userAnswer" class="form-control form-control-lg py-3 rounded-4" placeholder="请输入您的答案" :disabled="result">
              </div>

              <!-- Short Answer -->
              <div v-else-if="questions[currentQuestionIndex].type === 'SHORT_ANSWER'" class="mb-3">
                <textarea v-model="userAnswer" class="form-control form-control-lg rounded-4" rows="4" placeholder="请在此输入您的回答..." :disabled="result"></textarea>
              </div>
            </div>

            <div class="d-flex gap-3">
              <button @click="submitAnswer" class="btn btn-primary btn-lg flex-grow-1 py-3 shadow-sm" v-if="!result">
                <span>提交答案</span>
              </button>
              <button @click="nextQuestion" class="btn btn-primary btn-lg flex-grow-1 py-3 shadow-sm" v-if="result">
                <span>下一题</span>
                <i class="bi bi-arrow-right ms-2"></i>
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
                    <h6 class="fw-bold mb-1">{{ result.startsWith('回答正确') ? '太棒了!' : '还需要努力' }}</h6>
                    <p class="mb-0">{{ result }}</p>
                  </div>
                </div>

                <!-- Analysis Section -->
                <div class="analysis-panel p-4 rounded-4 border bg-white shadow-sm" v-if="questions[currentQuestionIndex].analysis">
                  <div class="d-flex align-items-center gap-2 mb-2 text-primary">
                    <i class="bi bi-lightbulb-fill"></i>
                    <h6 class="fw-bold mb-0">题目解析</h6>
                  </div>
                  <p class="mb-0 text-secondary lh-lg small">{{ questions[currentQuestionIndex].analysis }}</p>
                </div>
              </div>
            </transition>
          </div>
          
          <div v-else class="text-center py-5 fade-in">
            <div class="mb-4">
              <i class="bi bi-trophy display-1 text-warning"></i>
            </div>
            <h2 class="fw-bold mb-3">恭喜您，练习完成!</h2>
            <p class="text-secondary mb-5 fs-5">您已经完成了本分类下的所有题目</p>
            <button @click="questions = []" class="btn btn-primary btn-lg px-5 py-3 shadow-sm">
              <span>尝试其他分类</span>
            </button>
          </div>
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

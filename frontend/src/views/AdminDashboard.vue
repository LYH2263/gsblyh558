<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const authStore = useAuthStore()
const toast = useToast()
const isLoading = ref(false)
const activeTab = ref('categories')
const categories = ref([])
const questions = ref({ content: [], totalPages: 0, number: 0, totalElements: 0 })
const exams = ref([])

const newCategory = ref({ name: '', description: '' })
// Question Management State
const showQuestionModal = ref(false)
const editingQuestion = ref({
  id: null,
  content: '',
  type: 'SINGLE_CHOICE',
  category: { id: null },
  options: ['', '', '', ''],
  answer: '',
  analysis: '',
  difficulty: 3
})
const shortAnswerData = ref({
  standard: '',
  keywords: [{ text: '', weight: 50 }]
})

const addKeyword = () => {
  shortAnswerData.value.keywords.push({ text: '', weight: 10 })
}

const removeKeyword = (index) => {
  shortAnswerData.value.keywords.splice(index, 1)
}

const questionPage = ref(0)
const questionSize = ref(10)

const newExam = ref({ 
  title: '', 
  description: '', 
  totalScore: 100, 
  duration: 60,
  questions: [] 
})
const showExamModal = ref(false)
const examQuestions = ref([])
const availableQuestions = ref([])
const questionSearch = ref('')
const questionCategoryFilter = ref(null)

// Virtual List State
const poolScrollContainer = ref(null)
const poolScrollTop = ref(0)
const itemHeight = 110 // Estimated height per item

const virtualPool = computed(() => {
  const container = poolScrollContainer.value
  const questions = filteredQuestions.value
  
  // Calculate visible range
  const start = Math.floor(poolScrollTop.value / itemHeight)
  const visibleCount = container ? Math.ceil(container.clientHeight / itemHeight) : 10
  const end = start + visibleCount + 5 // Buffer of 5 items
  
  return {
    items: questions.slice(Math.max(0, start), end),
    paddingTop: Math.max(0, start) * itemHeight,
    totalHeight: questions.length * itemHeight
  }
})

const handlePoolScroll = (e) => {
  poolScrollTop.value = e.target.scrollTop
}

// Reset scroll when filters change
watch([questionSearch, questionCategoryFilter], () => {
  poolScrollTop.value = 0
  if (poolScrollContainer.value) {
    poolScrollContainer.value.scrollTop = 0
  }
})

// Delete Confirmation
const showDeleteModal = ref(false)
const deleteMessage = ref('')
const deleteAction = ref(null)

const confirmDelete = (message, action) => {
  deleteMessage.value = message
  deleteAction.value = action
  showDeleteModal.value = true
}

const executeDelete = async () => {
  if (deleteAction.value) await deleteAction.value()
  showDeleteModal.value = false
}

const openExamModal = (exam = null) => {
  if (exam) {
    newExam.value = JSON.parse(JSON.stringify(exam))
    // Transform backend questions format if needed
    if (!newExam.value.questions) newExam.value.questions = []
  } else {
    newExam.value = { 
      title: '', 
      description: '', 
      totalScore: 100, 
      duration: 60,
      questions: []
    }
  }
  showExamModal.value = true
  // Load available questions if not loaded
  if (availableQuestions.value.length === 0) {
    fetchAvailableQuestions()
  }
}

const fetchAvailableQuestions = async () => {
  // Fetch all questions for selection (optimize in real app with pagination/search API)
  const res = await request.get('/api/questions?size=1000')
  availableQuestions.value = res.data.data.content
}

const filteredQuestions = computed(() => {
  return availableQuestions.value.filter(q => {
    const matchContent = q.content.toLowerCase().includes(questionSearch.value.toLowerCase())
    const matchCategory = !questionCategoryFilter.value || q.category?.id === questionCategoryFilter.value
    // Exclude already added questions
    const notAdded = !newExam.value.questions.some(eq => eq.question.id === q.id)
    return matchContent && matchCategory && notAdded
  })
})

const addQuestionToExam = (question) => {
  newExam.value.questions.push({
    question: question,
    score: 5, // Default score
    orderNum: newExam.value.questions.length + 1
  })
}

const removeQuestionFromExam = (index) => {
  newExam.value.questions.splice(index, 1)
  // Re-order
  newExam.value.questions.forEach((q, idx) => q.orderNum = idx + 1)
}

const currentTotalScore = computed(() => {
  return newExam.value.questions.reduce((sum, q) => sum + (parseInt(q.score) || 0), 0)
})

const saveExam = async () => {
  // Frontend Validation
  if (!newExam.value.title || !newExam.value.title.trim()) {
    toast.warning('请输入考试标题')
    return
  }

  if (newExam.value.questions.length === 0) {
    toast.warning('请至少选择一道题目')
    return
  }

  if (currentTotalScore.value !== newExam.value.totalScore) {
    toast.error(`已选题目总分 (${currentTotalScore.value}) 与设定的考试总分 (${newExam.value.totalScore}) 不符，请调整题目分值或选题。`)
    return
  }

  try {
    if (newExam.value.id) {
      await request.put(`/api/exams/${newExam.value.id}`, newExam.value)
    } else {
      await request.post('/api/exams', newExam.value)
    }
    showExamModal.value = false
    fetchExams()
    toast.success('考试保存成功')
  } catch (error) {
    console.error('Failed to save exam:', error)
    toast.error('保存失败: ' + (error.response?.data?.message || error.message))
  }
}

const deleteExam = (id) => {
  confirmDelete('确定要删除这个考试吗？', async () => {
    try {
      await request.delete(`/api/exams/${id}`)
      fetchExams()
      toast.success('考试删除成功')
    } catch (error) {
      console.error('Failed to delete exam:', error)
      toast.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  })
}

const fetchCategories = async () => {
  try {
    const res = await request.get('/api/categories')
    categories.value = res.data.data
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

const addCategory = async () => {
  try {
    await request.post('/api/categories', newCategory.value)
    newCategory.value = { name: '', description: '' }
    fetchCategories()
    toast.success('分类添加成功')
  } catch (error) {
    console.error('Failed to add category:', error)
    toast.error('添加分类失败')
  }
}

const fetchQuestions = async (page = 0) => {
  try {
    const res = await request.get('/api/questions', {
      params: { 
        page, 
        size: questionSize.value,
        sortField: 'id',
        sortDir: 'asc'
      }
    })
    questions.value = res.data.data
  } catch (error) {
    console.error('Failed to fetch questions:', error)
  }
}

const openQuestionModal = (question = null) => {
  if (question) {
    editingQuestion.value = JSON.parse(JSON.stringify(question))
    if (!editingQuestion.value.options) editingQuestion.value.options = ['', '', '', '']
    if (!editingQuestion.value.difficulty) editingQuestion.value.difficulty = 3
    
    // Initialize short answer data if applicable
    if (editingQuestion.value.type === 'SHORT_ANSWER') {
      try {
        const parsed = JSON.parse(editingQuestion.value.answer)
        shortAnswerData.value = {
          standard: parsed.standard || '',
          keywords: parsed.keywords || [{ text: '', weight: 50 }]
        }
      } catch (e) {
        shortAnswerData.value = {
          standard: editingQuestion.value.answer,
          keywords: [{ text: '', weight: 50 }]
        }
      }
    }
  } else {
    editingQuestion.value = {
      id: null,
      content: '',
      type: 'SINGLE_CHOICE',
      category: { id: null },
      options: ['', '', '', ''],
      answer: '',
      analysis: '',
      difficulty: 3
    }
    shortAnswerData.value = {
      standard: '',
      keywords: [{ text: '', weight: 50 }]
    }
  }
  showQuestionModal.value = true
}

const saveQuestion = async () => {
  try {
    const payload = { ...editingQuestion.value }
    if (!payload.category.id) {
        toast.warning('请选择分类')
        return
    }

    if (payload.type === 'SHORT_ANSWER') {
      // Validate keywords
      if (!shortAnswerData.value.standard.trim()) {
        toast.warning('请输入简答题标准答案')
        return
      }
      const validKeywords = shortAnswerData.value.keywords.filter(k => k.text.trim())
      if (validKeywords.length === 0) {
        toast.warning('请至少添加一个关键要点')
        return
      }
      payload.answer = JSON.stringify({
        standard: shortAnswerData.value.standard,
        keywords: validKeywords
      })
    }
    
    if (payload.id) {
      await request.put(`/api/questions/${payload.id}`, payload)
    } else {
      await request.post('/api/questions', payload)
    }
    showQuestionModal.value = false
    fetchQuestions(questions.value.number)
    toast.success('题目保存成功')
  } catch (error) {
    console.error('Failed to save question:', error)
    toast.error('保存失败: ' + (error.response?.data?.message || error.message))
  }
}

const deleteQuestion = (id) => {
  confirmDelete('确定要删除这个题目吗？', async () => {
    try {
      await request.delete(`/api/questions/${id}`)
      fetchQuestions(questions.value.number)
      toast.success('题目删除成功')
    } catch (error) {
      console.error('Failed to delete question:', error)
      toast.error('删除失败')
    }
  })
}

const fetchExams = async () => {
  try {
    const res = await request.get('/api/exams')
    exams.value = res.data.data
  } catch (error) {
    console.error('Failed to fetch exams:', error)
  }
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

watch([showQuestionModal, showExamModal], ([newQ, newE]) => {
  if (newQ || newE) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

onMounted(async () => {
  isLoading.value = true
  await Promise.all([
    fetchCategories(),
    fetchQuestions(),
    fetchExams()
  ])
  isLoading.value = false
})
</script>

<template>
  <div class="admin-dashboard position-relative py-2">
    <div v-if="isLoading" class="loading-overlay glass-panel">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>
    
    <!-- Question Modal -->
    <div v-if="showQuestionModal" class="modal-backdrop-apple fade show" @click="showQuestionModal = false"></div>
    <div v-if="showQuestionModal" class="modal-apple fade show d-block" tabindex="-1">
      <div class="modal-dialog modal-dialog-lg">
        <div class="apple-modal-content border-0">
          <div class="apple-modal-header d-flex justify-content-between align-items-center">
            <h5 class="modal-title fw-bold">{{ editingQuestion.id ? '编辑题目' : '添加题目' }}</h5>
            <button type="button" class="btn-close" @click="showQuestionModal = false"></button>
          </div>
          <div class="apple-modal-body py-4">
            <form id="questionForm" @submit.prevent="saveQuestion">
              <div class="row g-4">
                <div class="col-md-12">
                  <label class="form-label">题目内容</label>
                  <textarea v-model="editingQuestion.content" class="form-control rounded-4" rows="3" placeholder="请输入题目正文..." required></textarea>
                </div>
                
                <div class="col-md-6">
                  <label class="form-label">题目类型</label>
                  <select v-model="editingQuestion.type" class="form-select rounded-4">
                    <option value="SINGLE_CHOICE">单选题</option>
                    <option value="MULTI_CHOICE">多选题</option>
                    <option value="TRUE_FALSE">判断题</option>
                    <option value="FILL_IN_BLANK">填空题</option>
                    <option value="SHORT_ANSWER">简答题</option>
                  </select>
                </div>
                
                <div class="col-md-6">
                  <label class="form-label">所属分类</label>
                  <select v-model="editingQuestion.category.id" class="form-select rounded-4" required>
                    <option :value="null" disabled>选择分类</option>
                    <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
                  </select>
                </div>

                <div class="col-md-6">
                  <label class="form-label">难度系数</label>
                  <div class="d-flex gap-2 align-items-center">
                    <div v-for="n in 5" :key="n" 
                         class="difficulty-star" 
                         :class="{ 'text-warning': n <= (editingQuestion.difficulty || 0), 'text-secondary opacity-25': n > (editingQuestion.difficulty || 0) }"
                         @click="editingQuestion.difficulty = n"
                         style="cursor: pointer; font-size: 1.5rem;">
                      <i :class="n <= (editingQuestion.difficulty || 0) ? 'bi bi-star-fill' : 'bi bi-star'"></i>
                    </div>
                  </div>
                </div>

                <!-- Options for Choice Questions -->
                <div class="col-md-12" v-if="['SINGLE_CHOICE', 'MULTI_CHOICE'].includes(editingQuestion.type)">
                  <label class="form-label">选项配置</label>
                  <div v-for="(opt, index) in editingQuestion.options" :key="index" class="input-group mb-3">
                    <span class="input-group-text bg-white border-end-0 fw-bold px-3" style="color: var(--apple-blue); border-color: var(--apple-gray-4); border-top-left-radius: 14px; border-bottom-left-radius: 14px;">{{ String.fromCharCode(65 + index) }}</span>
                    <input type="text" v-model="editingQuestion.options[index]" class="form-control border-start-0 py-3" style="border-top-right-radius: 14px; border-bottom-right-radius: 14px;" :placeholder="'选项内容 ' + (index + 1)">
                  </div>
                </div>

                <div class="col-md-12" v-if="editingQuestion.type === 'SHORT_ANSWER'">
                  <label class="form-label fw-bold">标准答案</label>
                  <textarea v-model="shortAnswerData.standard" class="form-control rounded-4 mb-4" rows="3" placeholder="请输入完整的标准答案内容..."></textarea>
                  
                  <div class="d-flex justify-content-between align-items-center mb-3">
                    <label class="form-label fw-bold mb-0">关键要点评分 (命中 60% 以上权重即为正确)</label>
                    <button type="button" class="btn btn-sm btn-outline-primary rounded-pill" @click="addKeyword">
                      <i class="bi bi-plus-circle me-1"></i>添加要点
                    </button>
                  </div>
                  
                  <div class="keyword-list">
                    <div v-for="(k, index) in shortAnswerData.keywords" :key="index" class="input-group mb-3 shadow-sm rounded-4 overflow-hidden">
                      <input type="text" v-model="k.text" class="form-control border-end-0 py-3" placeholder="关键要点文本">
                      <span class="input-group-text bg-white border-start-0 border-end-0 text-secondary small px-2">权重:</span>
                      <input type="number" v-model="k.weight" class="form-control border-start-0 border-end-0 py-3 text-center" style="max-width: 80px;" placeholder="权重">
                      <button class="btn btn-outline-danger border-start-0 px-3" type="button" @click="removeKeyword(index)" :disabled="shortAnswerData.keywords.length <= 1">
                        <i class="bi bi-trash"></i>
                      </button>
                    </div>
                  </div>
                </div>

                <div class="col-md-12" v-if="editingQuestion.type !== 'SHORT_ANSWER'">
                  <label class="form-label">正确答案</label>
                  <input type="text" v-model="editingQuestion.answer" class="form-control rounded-4 py-3" placeholder="单选填字母，多选如 AB，判断填 T/F" required>
                </div>

                <div class="col-md-12">
                  <label class="form-label">解析（可选）</label>
                  <textarea v-model="editingQuestion.analysis" class="form-control rounded-4" rows="2" placeholder="请输入题目解析，帮助学生理解..."></textarea>
                </div>
              </div>
            </form>
          </div>
          <div class="apple-modal-footer">
            <button type="button" class="btn btn-secondary flex-grow-1 py-3" @click="showQuestionModal = false">取消</button>
            <button type="submit" form="questionForm" class="btn btn-primary flex-grow-1 py-3 shadow-sm">保存题目</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Exam Modal -->
    <div v-if="showExamModal" class="modal-backdrop-apple fade show" @click="showExamModal = false"></div>
    <div v-if="showExamModal" class="modal-apple fade show d-block" tabindex="-1">
      <div class="modal-dialog modal-dialog-xl" style="max-width: 95%;">
        <div class="apple-modal-content border-0" style="height: 95vh;">
          <!-- Fixed Header: Basic Info -->
          <div class="apple-modal-header pb-2 pt-4 px-4">
            <div class="w-100">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="modal-title fw-bold h4">{{ newExam.id ? '编辑考试' : '创建新考试' }}</h5>
                <button type="button" class="btn-close" @click="showExamModal = false"></button>
              </div>
              
              <div class="p-3 bg-light rounded-4 shadow-sm border">
                <div class="row g-3 align-items-end">
                  <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary">考试标题</label>
                    <input type="text" v-model="newExam.title" class="form-control border-0 py-2 shadow-sm" placeholder="例如：Java 基础期末测试" required>
                  </div>
                  <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary">时长 (分钟)</label>
                    <input type="number" v-model="newExam.duration" class="form-control border-0 py-2 shadow-sm" required>
                  </div>
                  <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary">设定总分</label>
                    <input type="number" v-model="newExam.totalScore" class="form-control border-0 py-2 shadow-sm" required>
                  </div>
                  <div class="col-md-4">
                    <div class="p-2 rounded-3 text-center h-100 d-flex align-items-center justify-content-center border" :class="currentTotalScore === newExam.totalScore ? 'bg-success-soft text-success border-success' : 'bg-danger-soft text-danger border-danger'">
                      <span class="fw-bold">已选题目: {{ newExam.questions.length }} | 已选总分: {{ currentTotalScore }} / {{ newExam.totalScore }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="apple-modal-body py-0 px-4 overflow-hidden flex-grow-1">
            <div class="row g-0 h-100 border rounded-4 overflow-hidden bg-white shadow-inner">
              <!-- Left: Question Pool -->
              <div class="col-md-7 border-end d-flex flex-column h-100">
                <div class="p-3 bg-light border-bottom">
                  <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold mb-0">1. 从题库选择</h6>
                    <span class="badge bg-primary-soft text-primary">{{ filteredQuestions.length }} 题符合条件</span>
                  </div>
                  <div class="row g-2">
                    <div class="col-6">
                      <select v-model="questionCategoryFilter" class="form-select form-select-sm border-0 shadow-sm">
                        <option :value="null">所有分类</option>
                        <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
                      </select>
                    </div>
                    <div class="col-6">
                      <input type="text" v-model="questionSearch" class="form-control form-select-sm border-0 shadow-sm" placeholder="搜索题目内容...">
                    </div>
                  </div>
                </div>
                
                <div class="flex-grow-1 overflow-auto p-2 scroll-container" 
                     ref="poolScrollContainer"
                     @scroll="handlePoolScroll"
                     style="background: #fdfdfd;">
                  <div :style="{ height: virtualPool.totalHeight + 'px', position: 'relative' }">
                    <div :style="{ transform: `translateY(${virtualPool.paddingTop}px)` }">
                      <div class="list-group list-group-flush gap-2">
                        <div v-for="q in virtualPool.items" :key="q.id" 
                                class="list-group-item list-group-item-action border-0 p-3 rounded-4 shadow-sm mb-1 transfer-item available"
                                :style="{ height: itemHeight + 'px', overflow: 'hidden' }"
                                @click="addQuestionToExam(q)">
                          <div class="d-flex w-100 justify-content-between align-items-start mb-1">
                            <span class="badge bg-light text-secondary small">{{ getQuestionTypeLabel(q.type) }}</span>
                            <div class="difficulty-dots">
                              <i v-for="n in 5" :key="n" class="bi bi-circle-fill" :class="{ 'text-warning': n <= q.difficulty, 'text-light': n > q.difficulty }"></i>
                            </div>
                          </div>
                          <h6 class="mb-1 fw-bold small text-dark lh-base text-truncate-2">{{ q.content }}</h6>
                          <div class="d-flex justify-content-between align-items-center mt-auto">
                            <small class="text-secondary text-truncate" style="max-width: 80%;"><i class="bi bi-folder me-1"></i>{{ q.category?.name }}</small>
                            <i class="bi bi-plus-circle-fill text-primary h5 mb-0"></i>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-if="filteredQuestions.length === 0" class="text-center py-5 text-secondary">
                    <i class="bi bi-search display-6 d-block mb-3 opacity-25"></i>
                    <p class="small">未找到符合条件的题目</p>
                  </div>
                </div>
              </div>

              <!-- Right: Selected Questions -->
              <div class="col-md-5 d-flex flex-column h-100">
                <div class="p-3 bg-light border-bottom">
                  <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold mb-0">2. 已选题目 ({{ newExam.questions.length }})</h6>
                    <button class="btn btn-sm btn-link text-danger p-0 text-decoration-none small" @click="newExam.questions = []">清空全部</button>
                  </div>
                  <div class="small text-secondary">点击题目移除，或调整右侧分值</div>
                </div>

                <div class="flex-grow-1 overflow-auto p-2" style="background: #fdfdfd;">
                  <div class="list-group list-group-flush gap-2">
                    <div v-for="(q, index) in newExam.questions" :key="index" 
                         class="list-group-item list-group-item-action border-0 p-3 bg-white shadow-sm rounded-4 mb-1 transfer-item selected"
                         @click="removeQuestionFromExam(index)">
                      <div class="d-flex justify-content-between align-items-start gap-2 mb-1">
                        <div class="d-flex align-items-center gap-2">
                          <span class="badge bg-primary-soft text-primary">{{ index + 1 }}</span>
                          <span class="badge bg-light text-secondary small">{{ getQuestionTypeLabel(q.question.type) }}</span>
                        </div>
                        <div class="score-input-group d-flex align-items-center gap-2" @click.stop>
                          <label class="small text-secondary fw-bold">分值:</label>
                          <input type="number" v-model="q.score" class="form-control form-control-sm border-0 bg-light rounded-3 text-center fw-bold" style="width: 50px;">
                        </div>
                      </div>
                      <h6 class="mb-1 fw-bold small text-dark lh-base text-truncate-2">{{ q.question.content }}</h6>
                      <div class="text-end mt-auto">
                        <i class="bi bi-dash-circle-fill text-danger h5 mb-0"></i>
                      </div>
                    </div>
                    <div v-if="newExam.questions.length === 0" class="text-center py-5 text-secondary">
                      <i class="bi bi-arrow-left-circle display-6 d-block mb-3 opacity-25"></i>
                      <p class="small">请从左侧选择题目加入考试</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Fixed Footer: Actions -->
          <div class="apple-modal-footer p-3 gap-3">
            <button type="button" class="btn btn-secondary px-5 py-2 rounded-4" @click="showExamModal = false">取消</button>
            <button type="button" class="btn btn-primary px-5 py-2 rounded-4 shadow-sm fw-bold" @click="saveExam">
              <i class="bi bi-check-lg me-2"></i>保存并发布考试
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="d-flex justify-content-between align-items-end mb-5 fade-in">
      <div>
        <h1 class="apple-title mb-1">管理后台</h1>
        <p class="text-secondary mb-0">系统资源、题库与考试流程管理中心</p>
      </div>
    </div>

    <div class="row">
      <div class="col-12">
        <div class="glass-panel p-4 p-md-5 fade-in-up" style="border-radius: 32px;">
          <ul class="nav nav-pills gap-2 mb-5 p-2 bg-light rounded-4 d-inline-flex align-items-center">
            <li class="nav-item">
              <button class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center" :class="{ active: activeTab === 'categories' }" @click="activeTab = 'categories'">分类管理</button>
            </li>
            <li class="nav-item">
              <button class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center" :class="{ active: activeTab === 'questions' }" @click="activeTab = 'questions'">题库管理</button>
            </li>
            <li class="nav-item">
              <button class="nav-link px-4 py-2 rounded-3 fw-bold d-flex align-items-center" :class="{ active: activeTab === 'exams' }" @click="activeTab = 'exams'">考试管理</button>
            </li>
          </ul>

          <div v-if="activeTab === 'categories'" class="fade-in">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h3 class="fw-bold h4 mb-0">分类管理</h3>
            </div>
            
            <div class="p-4 bg-light rounded-4 mb-5">
              <h6 class="fw-bold mb-3">新增分类</h6>
              <form @submit.prevent="addCategory">
                <div class="row g-3">
                  <div class="col-md-4">
                    <input type="text" v-model="newCategory.name" class="form-control border-0 py-3" placeholder="分类名称" required>
                  </div>
                  <div class="col-md-6">
                    <input type="text" v-model="newCategory.description" class="form-control border-0 py-3" placeholder="简单描述一下这个分类">
                  </div>
                  <div class="col-md-2">
                    <button type="submit" class="btn btn-primary w-100 py-3 shadow-sm">添加</button>
                  </div>
                </div>
              </form>
            </div>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead>
                  <tr>
                    <th width="80">ID</th>
                    <th>分类名称</th>
                    <th>描述</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="cat in categories" :key="cat.id">
                    <td class="text-secondary small">#{{ cat.id }}</td>
                    <td class="fw-bold text-primary">{{ cat.name }}</td>
                    <td class="text-secondary">{{ cat.description || '无描述' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-if="activeTab === 'questions'" class="fade-in">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h3 class="fw-bold h4 mb-0">题库管理</h3>
              <button class="btn btn-primary px-4 py-2 shadow-sm" @click="openQuestionModal(null)">
                <i class="bi bi-plus-lg me-2"></i>添加题目
              </button>
            </div>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead>
                  <tr>
                    <th width="80">ID</th>
                    <th>题目内容</th>
                    <th width="120">题型</th>
                    <th width="120">分类</th>
                    <th width="150">难度</th>
                    <th width="180">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="q in questions.content" :key="q.id">
                    <td class="text-secondary small">#{{ q.id }}</td>
                    <td>
                      <div class="text-truncate fw-600" style="max-width: 300px;" :title="q.content">
                        {{ q.content }}
                      </div>
                    </td>
                    <td>
                      <span class="badge bg-primary-soft text-primary">{{ getQuestionTypeLabel(q.type) }}</span>
                    </td>
                    <td><span class="text-secondary small">{{ q.category?.name }}</span></td>
                    <td>
                      <div class="text-warning small">
                        <i v-for="n in q.difficulty" :key="n" class="bi bi-star-fill me-1"></i>
                      </div>
                    </td>
                    <td>
                      <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-light text-primary px-3" @click="openQuestionModal(q)">编辑</button>
                        <button class="btn btn-sm btn-light text-danger px-3" @click="deleteQuestion(q.id)">删除</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            
            <!-- Pagination -->
            <nav v-if="questions.totalPages > 1" class="mt-5">
              <ul class="pagination justify-content-center gap-2">
                <li class="page-item" :class="{ disabled: questions.first }">
                  <button class="page-link rounded-3 border-0 bg-light px-4" @click="fetchQuestions(questions.number - 1)">上一页</button>
                </li>
                <li class="page-item active">
                  <span class="page-link rounded-3 border-0 px-4">第 {{ questions.number + 1 }} / {{ questions.totalPages }} 页</span>
                </li>
                <li class="page-item" :class="{ disabled: questions.last }">
                  <button class="page-link rounded-3 border-0 bg-light px-4" @click="fetchQuestions(questions.number + 1)">下一页</button>
                </li>
              </ul>
            </nav>
          </div>

          <div v-if="activeTab === 'exams'" class="fade-in">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h3 class="fw-bold h4 mb-0">考试管理</h3>
              <button class="btn btn-primary px-4 py-2 shadow-sm" @click="openExamModal(null)">
                <i class="bi bi-plus-lg me-2"></i>创建考试
              </button>
            </div>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead>
                  <tr>
                    <th width="80">ID</th>
                    <th>考试标题</th>
                    <th width="120">限时</th>
                    <th width="120">总分</th>
                    <th width="120">题数</th>
                    <th width="180">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="exam in exams" :key="exam.id">
                    <td class="text-secondary small">#{{ exam.id }}</td>
                    <td class="fw-bold">{{ exam.title }}</td>
                    <td><span class="text-secondary small"><i class="bi bi-clock me-1"></i>{{ exam.duration }} 分钟</span></td>
                    <td><span class="badge bg-success-soft text-success">{{ exam.totalScore }} 分</span></td>
                    <td><span class="text-secondary small">{{ exam.questions ? exam.questions.length : 0 }} 题</span></td>
                    <td>
                      <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-light text-primary px-3" @click="openExamModal(exam)">编辑</button>
                        <button class="btn btn-sm btn-light text-danger px-3" @click="deleteExam(exam.id)">删除</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal-backdrop-apple fade show" @click="showDeleteModal = false"></div>
    <div v-if="showDeleteModal" class="modal-apple fade show d-block" tabindex="-1">
      <div class="modal-dialog">
        <div class="apple-modal-content border-0 p-3">
          <div class="apple-modal-header">
            <h5 class="modal-title fw-bold">确认删除此内容？</h5>
          </div>
          <div class="apple-modal-body py-4 text-center">
            <p class="text-secondary mb-0">{{ deleteMessage }} 此操作不可撤销。</p>
          </div>
          <div class="apple-modal-footer">
            <button type="button" class="btn btn-secondary flex-grow-1 py-3" @click="showDeleteModal = false">取消</button>
            <button type="button" class="btn btn-danger flex-grow-1 py-3 shadow-sm" @click="executeDelete">确认删除</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.bg-success-soft {
  background-color: rgba(52, 199, 89, 0.1);
}

.bg-danger-soft {
  background-color: rgba(255, 59, 48, 0.1);
}

.bg-primary-soft {
  background-color: rgba(0, 122, 255, 0.1);
}

.fw-600 {
  font-weight: 600;
}

.difficulty-star:hover {
  transform: scale(1.2);
  transition: transform 0.2s;
}

/* Redesigned Exam Modal Styles */
.transfer-item {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: 1px solid transparent !important;
}

.transfer-item:hover {
  transform: translateY(-2px);
  z-index: 1;
}

.transfer-item.available:hover {
  border-color: var(--apple-blue) !important;
  background-color: rgba(0, 122, 255, 0.02) !important;
}

.transfer-item.selected:hover {
  border-color: var(--apple-red) !important;
  background-color: rgba(255, 59, 48, 0.02) !important;
}

.difficulty-dots {
  display: flex;
  gap: 2px;
  font-size: 8px;
}

.text-truncate-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scroll-container {
  scrollbar-width: thin;
  scrollbar-color: var(--apple-gray-4) transparent;
}

.scroll-container::-webkit-scrollbar {
  width: 6px;
}

.scroll-container::-webkit-scrollbar-thumb {
  background-color: var(--apple-gray-4);
  border-radius: 10px;
}

.score-input-group input::-webkit-outer-spin-button,
.score-input-group input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.nav-pills .nav-link {
  color: var(--text-secondary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-pills .nav-link.active {
  background-color: white;
  color: var(--apple-blue);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
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

<style scoped>
/* Scoped styles removed in favor of global Apple-style assets/main.css */
</style>

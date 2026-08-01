import request from '../utils/request'

// 错题本接口
export const wrongQuestionApi = {
  list() {
    return request.get('/api/wrong-questions')
  }
}

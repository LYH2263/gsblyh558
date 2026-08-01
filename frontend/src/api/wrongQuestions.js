import request from '../utils/request'

export const listWrongQuestions = () => request.get('/api/wrong-questions').then(res => res.data)

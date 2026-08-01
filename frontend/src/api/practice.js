import request from '../utils/request'

export const submitPractice = (payload) =>
  request.post('/api/practice/submit', payload).then(res => res.data)

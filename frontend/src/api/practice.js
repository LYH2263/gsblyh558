import request from '../utils/request'

export const submitPractice = (data) => request.post('/api/practice/submit', data).then(res => res.data)

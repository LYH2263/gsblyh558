import request from '../utils/request'

export const signin = (data) => request.post('/api/auth/signin', data).then(res => res.data)

export const signup = (data) => request.post('/api/auth/signup', data).then(res => res.data)

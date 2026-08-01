import request from '../utils/request'

export const listCategories = () => request.get('/api/categories').then(res => res.data)

export const createCategory = (data) => request.post('/api/categories', data).then(res => res.data)

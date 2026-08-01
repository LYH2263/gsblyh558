import request from '../utils/request'

export const listCategories = () => request.get('/api/categories').then(res => res.data)
export const createCategory = (payload) => request.post('/api/categories', payload).then(res => res.data)
export const updateCategory = (id, payload) => request.put(`/api/categories/${id}`, payload).then(res => res.data)
export const deleteCategory = (id) => request.delete(`/api/categories/${id}`).then(res => res.data)

import { defineStore } from 'pinia'
import request from '../utils/request'

const API_URL = '/api/auth/'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null
  }),
  actions: {
    async login(username, password) {
      const response = await request.post(API_URL + 'signin', {
        username,
        password
      })
      if (response.data.data && response.data.data.token) {
        localStorage.setItem('user', JSON.stringify(response.data.data))
        this.user = response.data.data
      }
      return response.data
    },
    logout() {
      localStorage.removeItem('user')
      this.user = null
    },
    async register(user) {
      return request.post(API_URL + 'signup', user)
    }
  }
})

import { defineStore } from 'pinia'
import { authApi } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null
  }),
  actions: {
    async login(username, password) {
      const response = await authApi.signin(username, password)
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
      return authApi.signup(user)
    }
  }
})

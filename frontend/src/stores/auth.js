import { defineStore } from 'pinia'
import { signin, signup } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null
  }),
  actions: {
    async login(username, password) {
      const response = await signin({ username, password })
      if (response.data && response.data.token) {
        localStorage.setItem('user', JSON.stringify(response.data))
        this.user = response.data
      }
      return response
    },
    logout() {
      localStorage.removeItem('user')
      this.user = null
    },
    async register(user) {
      return signup(user)
    }
  }
})

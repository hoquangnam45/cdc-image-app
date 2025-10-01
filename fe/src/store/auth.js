import { defineStore } from 'pinia'
import axios from 'axios'

axios.defaults.withCredentials = true

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthenticated: false,
    expiresAt: null,
    refreshTimerId: null,
  }),
  actions: {
    async login(payload) {
      const res = await axios.post('/api/auth/login', payload)
      this._handleAuthResponse(res.data?.data)
    },
    async register(payload) {
      const res = await axios.post('/api/auth/register', payload)
      this._handleAuthResponse(res.data?.data)
    },
    async refresh() {
      await axios.post('/api/auth/refresh')
      // Backend sets cookies and returns new times; we can re-fetch list or rely on timer
    },
    logoutLocal() {
      this.isAuthenticated = false
      this.expiresAt = null
      if (this.refreshTimerId) {
        clearInterval(this.refreshTimerId)
        this.refreshTimerId = null
      }
    },
    _handleAuthResponse(loginResponse) {
      // loginResponse contains accessTokenExpireAt, refreshTokenExpireAt
      this.isAuthenticated = true
      this.expiresAt = loginResponse?.accessTokenExpireAt || null
      if (this.refreshTimerId) clearInterval(this.refreshTimerId)
      // schedule refresh at 60s before expiry, fallback to 60s interval
      const schedule = () => {
        if (!this.expiresAt) return
        const expiryMs = new Date(this.expiresAt).getTime()
        const now = Date.now()
        const delay = Math.max(5_000, expiryMs - now - 60_000)
        this.refreshTimerId = setTimeout(async () => {
          try {
            await this.refresh()
          } catch (_) {
            this.logoutLocal()
            return
          }
          schedule()
        }, delay)
      }
      schedule()
    },
  },
})



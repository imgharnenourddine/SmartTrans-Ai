import type { AuthUser } from '../types/auth'

const TOKEN_KEY = 'smarttrans.accessToken'
const USER_KEY = 'smarttrans.user'

export const tokenStorage = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  },

  clearToken(): void {
    localStorage.removeItem(TOKEN_KEY)
  },

  getUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw) as AuthUser
    } catch {
      return null
    }
  },

  setUser(user: AuthUser): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },

  clearUser(): void {
    localStorage.removeItem(USER_KEY)
  },

  clear(): void {
    this.clearToken()
    this.clearUser()
  },
}

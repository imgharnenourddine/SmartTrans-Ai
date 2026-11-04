import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { authService } from '../services/authService'
import type { AuthUser, LoginRequest, RoleCode } from '../types/auth'
import { tokenStorage } from '../utils/tokenStorage'

interface AuthContextValue {
  user: AuthUser | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (payload: LoginRequest) => Promise<void>
  logout: () => void
  hasRole: (...roles: RoleCode[]) => boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function readInitialAuth(): { user: AuthUser | null; token: string | null } {
  const token = tokenStorage.getToken()
  const user = tokenStorage.getUser()
  if (!token || !user) {
    tokenStorage.clear()
    return { user: null, token: null }
  }
  return { user, token }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const initial = readInitialAuth()
  const [user, setUser] = useState<AuthUser | null>(initial.user)
  const [token, setToken] = useState<string | null>(initial.token)
  const [isLoading, setIsLoading] = useState(false)

  const login = useCallback(async (payload: LoginRequest) => {
    setIsLoading(true)
    try {
      const response = await authService.login(payload)
      const nextUser: AuthUser = {
        userId: response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        roleCodes: response.roleCodes,
      }
      tokenStorage.setToken(response.accessToken)
      tokenStorage.setUser(nextUser)
      setToken(response.accessToken)
      setUser(nextUser)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    setToken(null)
    setUser(null)
  }, [])

  const hasRole = useCallback(
    (...roles: RoleCode[]) => {
      if (!user) {
        return false
      }
      return roles.some((role) => user.roleCodes.includes(role))
    },
    [user],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      isLoading,
      login,
      logout,
      hasRole,
    }),
    [user, token, isLoading, login, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

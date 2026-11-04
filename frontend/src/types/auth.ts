export type RoleCode = 'ADMIN' | 'TRANSPORT_MANAGER' | 'DRIVER'

export interface AuthUser {
  userId: string
  email: string
  firstName: string
  lastName: string
  roleCodes: RoleCode[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresInMs: number
  userId: string
  email: string
  firstName: string
  lastName: string
  roleCodes: RoleCode[]
}

export interface ApiErrorResponse {
  timestamp?: string
  status: number
  error: string
  message: string
  path?: string
  fieldErrors?: Record<string, string>
}

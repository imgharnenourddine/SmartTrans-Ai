import type { RoleCode } from './auth'
import { ROLE_LABELS } from './role'

export interface UserAccount {
  id: string
  email: string
  firstName: string
  lastName: string
  phone: string | null
  enabled: boolean
  lastLoginAt: string | null
  roleCodes: RoleCode[]
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string | null
  roleCodes: RoleCode[]
}

export interface UpdateUserRequest {
  email?: string
  firstName?: string
  lastName?: string
  phone?: string | null
  enabled?: boolean
  roleCodes?: RoleCode[]
}

export function roleLabels(roleCodes: RoleCode[]): string {
  return roleCodes.map((code) => ROLE_LABELS[code] ?? code).join(', ')
}

export function matchesUserSearch(user: UserAccount, search: string): boolean {
  const query = search.trim().toLowerCase()
  if (!query) {
    return true
  }
  const haystack = [
    user.email,
    user.firstName,
    user.lastName,
    user.phone,
    ...user.roleCodes,
    ...user.roleCodes.map((code) => ROLE_LABELS[code]),
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return haystack.includes(query)
}

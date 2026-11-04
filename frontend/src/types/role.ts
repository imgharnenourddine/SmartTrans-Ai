import type { RoleCode } from './auth'

export interface Role {
  id: string
  code: RoleCode
  label: string
  createdAt: string
}

export const ROLE_LABELS: Record<RoleCode, string> = {
  ADMIN: 'Administrateur',
  TRANSPORT_MANAGER: 'Responsable transport',
  DRIVER: 'Chauffeur',
}

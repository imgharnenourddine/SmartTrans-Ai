import { apiClient } from '../api/client'
import type { Role } from '../types/role'

export const roleService = {
  async list(): Promise<Role[]> {
    const { data } = await apiClient.get<Role[]>('/roles')
    return data
  },
}

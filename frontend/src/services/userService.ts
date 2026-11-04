import { apiClient } from '../api/client'
import type { CreateUserRequest, UpdateUserRequest, UserAccount } from '../types/user'

export const userService = {
  async list(): Promise<UserAccount[]> {
    const { data } = await apiClient.get<UserAccount[]>('/users')
    return data
  },

  async getById(id: string): Promise<UserAccount> {
    const { data } = await apiClient.get<UserAccount>(`/users/${id}`)
    return data
  },

  async create(payload: CreateUserRequest): Promise<UserAccount> {
    const { data } = await apiClient.post<UserAccount>('/users', payload)
    return data
  },

  async update(id: string, payload: UpdateUserRequest): Promise<UserAccount> {
    const { data } = await apiClient.patch<UserAccount>(`/users/${id}`, payload)
    return data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/users/${id}`)
  },
}

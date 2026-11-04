import { apiClient } from '../api/client'
import type { Client, CreateClientRequest, UpdateClientRequest } from '../types/client'

export const clientService = {
  async list(): Promise<Client[]> {
    const { data } = await apiClient.get<Client[]>('/clients')
    return data
  },

  async getById(id: string): Promise<Client> {
    const { data } = await apiClient.get<Client>(`/clients/${id}`)
    return data
  },

  async create(payload: CreateClientRequest): Promise<Client> {
    const { data } = await apiClient.post<Client>('/clients', payload)
    return data
  },

  async update(id: string, payload: UpdateClientRequest): Promise<Client> {
    const { data } = await apiClient.patch<Client>(`/clients/${id}`, payload)
    return data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/clients/${id}`)
  },
}

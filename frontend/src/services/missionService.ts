import { apiClient } from '../api/client'
import type { CreateMissionRequest, Mission, MissionStatus, UpdateMissionRequest } from '../types/mission'

export const missionService = {
  async list(): Promise<Mission[]> {
    const { data } = await apiClient.get<Mission[]>('/missions')
    return data
  },

  async getById(id: string): Promise<Mission> {
    const { data } = await apiClient.get<Mission>(`/missions/${id}`)
    return data
  },

  async create(payload: CreateMissionRequest): Promise<Mission> {
    const { data } = await apiClient.post<Mission>('/missions', payload)
    return data
  },

  async update(id: string, payload: UpdateMissionRequest): Promise<Mission> {
    const { data } = await apiClient.patch<Mission>(`/missions/${id}`, payload)
    return data
  },

  async updateStatus(id: string, status: MissionStatus): Promise<Mission> {
    const { data } = await apiClient.patch<Mission>(`/missions/${id}/status`, { status })
    return data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/missions/${id}`)
  },
}

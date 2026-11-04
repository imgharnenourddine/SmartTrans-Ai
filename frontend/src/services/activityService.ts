import { apiClient } from '../api/client'
import type { ActivityRecord } from '../types/activity'

export const activityService = {
  async list(): Promise<ActivityRecord[]> {
    const { data } = await apiClient.get<ActivityRecord[]>('/activities')
    return data
  },

  async getById(id: string): Promise<ActivityRecord> {
    const { data } = await apiClient.get<ActivityRecord>(`/activities/${id}`)
    return data
  },
}

import { apiClient } from '../api/client'
import type { AssistantAskResponse } from '../types/assistant'

export const assistantService = {
  async ask(message: string): Promise<AssistantAskResponse> {
    const { data } = await apiClient.post<AssistantAskResponse>('/assistant/ask', { message })
    return data
  },
}

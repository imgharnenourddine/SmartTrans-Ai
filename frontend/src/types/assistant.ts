export type AssistantCitationKind = 'MISSION' | 'DRIVER' | 'VEHICLE' | 'CLIENT'

export interface AssistantCitation {
  kind: AssistantCitationKind
  id: string
  title: string
  snippet: string
}

export interface AssistantAskResponse {
  answer: string
  mode: 'GROUNDED' | 'LLM'
  citations: AssistantCitation[]
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  mode?: AssistantAskResponse['mode']
  citations?: AssistantCitation[]
}

export function citationPath(citation: AssistantCitation): string {
  switch (citation.kind) {
    case 'MISSION':
      return `/missions/${citation.id}`
    case 'DRIVER':
      return `/drivers/${citation.id}`
    case 'VEHICLE':
      return `/vehicles/${citation.id}`
    case 'CLIENT':
      return `/clients/${citation.id}`
    default:
      return '/dashboard'
  }
}

export function citationKindLabel(kind: AssistantCitationKind): string {
  switch (kind) {
    case 'MISSION':
      return 'Mission'
    case 'DRIVER':
      return 'Chauffeur'
    case 'VEHICLE':
      return 'Véhicule'
    case 'CLIENT':
      return 'Client'
    default:
      return kind
  }
}

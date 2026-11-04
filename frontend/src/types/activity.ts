export type ActivityAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'STATUS_CHANGE' | 'LOGIN'

export interface ActivityRecord {
  id: string
  actorUserId: string | null
  action: ActivityAction
  entityType: string
  entityId: string | null
  metadata: Record<string, unknown> | null
  createdAt: string
}

export interface CreateActivityRequest {
  actorUserId?: string | null
  action: ActivityAction
  entityType: string
  entityId?: string | null
  metadata?: Record<string, unknown> | null
}

export interface UpdateActivityRequest {
  actorUserId?: string | null
  action?: ActivityAction
  entityType?: string
  entityId?: string | null
  metadata?: Record<string, unknown> | null
}

export const ACTIVITY_ACTION_LABELS: Record<ActivityAction, string> = {
  CREATE: 'Création',
  UPDATE: 'Modification',
  DELETE: 'Suppression',
  STATUS_CHANGE: 'Changement de statut',
  LOGIN: 'Connexion',
}

export const ENTITY_TYPE_OPTIONS = ['MISSION', 'DRIVER', 'VEHICLE', 'CLIENT', 'USER'] as const

export type KnownEntityType = (typeof ENTITY_TYPE_OPTIONS)[number]

export const ENTITY_TYPE_LABELS: Record<KnownEntityType, string> = {
  MISSION: 'Mission',
  DRIVER: 'Chauffeur',
  VEHICLE: 'Véhicule',
  CLIENT: 'Client',
  USER: 'Utilisateur',
}

export function entityTypeLabel(entityType: string): string {
  if ((ENTITY_TYPE_OPTIONS as readonly string[]).includes(entityType)) {
    return ENTITY_TYPE_LABELS[entityType as KnownEntityType]
  }
  return entityType
}

export function entityDetailPath(entityType: string, entityId: string): string | null {
  switch (entityType) {
    case 'MISSION':
      return `/missions/${entityId}`
    case 'DRIVER':
      return `/drivers/${entityId}`
    case 'VEHICLE':
      return `/vehicles/${entityId}`
    case 'CLIENT':
      return `/clients/${entityId}`
    case 'USER':
      return `/users/${entityId}`
    default:
      return null
  }
}

function metaString(metadata: Record<string, unknown> | null, key: string): string | null {
  const value = metadata?.[key]
  if (typeof value !== 'string' || value.trim() === '') {
    return null
  }
  return value.trim()
}

export function activitySubject(activity: ActivityRecord): string {
  const metadata = activity.metadata
  const fallback = activity.entityId ?? entityTypeLabel(activity.entityType)
  switch (activity.entityType) {
    case 'MISSION':
      return metaString(metadata, 'reference') ?? fallback
    case 'DRIVER': {
      const name = [metaString(metadata, 'firstName'), metaString(metadata, 'lastName')].filter(Boolean).join(' ')
      return name || metaString(metadata, 'licenseNumber') || fallback
    }
    case 'VEHICLE':
      return metaString(metadata, 'registrationNumber') ?? fallback
    case 'CLIENT':
      return metaString(metadata, 'companyName') ?? fallback
    case 'USER':
      return metaString(metadata, 'email') ?? fallback
    default:
      return fallback
  }
}

export function matchesActivitySearch(activity: ActivityRecord, search: string): boolean {
  const query = search.trim().toLowerCase()
  if (!query) {
    return true
  }
  const haystack = [
    activity.action,
    ACTIVITY_ACTION_LABELS[activity.action],
    activity.entityType,
    entityTypeLabel(activity.entityType),
    activitySubject(activity),
    activity.entityId,
    activity.actorUserId,
    activity.metadata ? JSON.stringify(activity.metadata) : '',
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return haystack.includes(query)
}

export function formatActivityMetadata(metadata: Record<string, unknown> | null): string {
  if (!metadata || Object.keys(metadata).length === 0) {
    return '—'
  }
  return JSON.stringify(metadata, null, 2)
}

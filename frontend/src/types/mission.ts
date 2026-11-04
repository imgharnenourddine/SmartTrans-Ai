export type MissionStatus = 'PLANIFIED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface Mission {
  id: string
  reference: string
  clientId: string
  clientName: string | null
  driverId: string | null
  driverFirstName: string | null
  driverLastName: string | null
  vehicleId: string | null
  vehicleRegistrationNumber: string | null
  createdById: string | null
  departureLocation: string
  destination: string
  scheduledAt: string
  startedAt: string | null
  completedAt: string | null
  weightKg: number | null
  cargoType: string | null
  observations: string | null
  status: MissionStatus
  createdAt: string
  updatedAt: string
}

export interface CreateMissionRequest {
  reference: string
  clientId: string
  driverId?: string | null
  vehicleId?: string | null
  departureLocation: string
  destination: string
  scheduledAt: string
  weightKg?: number | null
  cargoType?: string | null
  observations?: string | null
}

export interface UpdateMissionRequest {
  reference?: string
  clientId?: string
  driverId?: string | null
  vehicleId?: string | null
  departureLocation?: string
  destination?: string
  scheduledAt?: string
  weightKg?: number | null
  cargoType?: string | null
  observations?: string | null
  status?: MissionStatus
}

export const MISSION_STATUS_LABELS: Record<MissionStatus, string> = {
  PLANIFIED: 'Planifiée',
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Terminée',
  CANCELLED: 'Annulée',
}

export const MISSION_STATUS_ACTIONS: Record<MissionStatus, string> = {
  PLANIFIED: 'Planifier',
  IN_PROGRESS: 'Démarrer',
  COMPLETED: 'Terminer',
  CANCELLED: 'Annuler',
}

const ALLOWED_TRANSITIONS: Record<MissionStatus, MissionStatus[]> = {
  PLANIFIED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

export function allowedStatusTransitions(status: MissionStatus): MissionStatus[] {
  return ALLOWED_TRANSITIONS[status]
}

export function canDeleteMission(status: MissionStatus): boolean {
  return status === 'PLANIFIED' || status === 'CANCELLED'
}

export function canReassignMission(status: MissionStatus): boolean {
  return status === 'PLANIFIED'
}

export function driverDisplayName(mission: Mission): string {
  if (!mission.driverFirstName && !mission.driverLastName) {
    return '—'
  }
  return `${mission.driverFirstName ?? ''} ${mission.driverLastName ?? ''}`.trim()
}

export function matchesMissionSearch(mission: Mission, search: string): boolean {
  const query = search.trim().toLowerCase()
  if (!query) {
    return true
  }
  const haystack = [
    mission.reference,
    mission.clientName,
    mission.departureLocation,
    mission.destination,
    mission.cargoType,
    mission.vehicleRegistrationNumber,
    mission.driverFirstName,
    mission.driverLastName,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return haystack.includes(query)
}

export function toDatetimeLocalValue(iso: string | null | undefined): string {
  if (!iso) {
    return ''
  }
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function fromDatetimeLocalValue(value: string): string {
  return new Date(value).toISOString()
}

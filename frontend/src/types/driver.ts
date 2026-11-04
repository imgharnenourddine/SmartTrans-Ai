export type DriverStatus = 'AVAILABLE' | 'ON_MISSION' | 'UNAVAILABLE' | 'SUSPENDED'

export interface Driver {
  id: string
  userId: string | null
  firstName: string
  lastName: string
  phone: string
  licenseNumber: string
  licenseExpiresAt: string
  experienceYears: number | null
  status: DriverStatus
  licenseExpired: boolean
  licenseExpiringSoon: boolean
  daysUntilLicenseExpiry: number
  availableForMission: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateDriverRequest {
  userId?: string | null
  firstName: string
  lastName: string
  phone: string
  licenseNumber: string
  licenseExpiresAt: string
  experienceYears?: number | null
  status?: DriverStatus
}

export interface UpdateDriverRequest {
  userId?: string | null
  firstName?: string
  lastName?: string
  phone?: string
  licenseNumber?: string
  licenseExpiresAt?: string
  experienceYears?: number | null
  status?: DriverStatus
}

export interface UpdateDriverAvailabilityRequest {
  status: 'AVAILABLE' | 'UNAVAILABLE'
}

export interface DriverSearchParams {
  search?: string
  status?: DriverStatus
  licenseExpired?: boolean
  availableOnly?: boolean
}

export const DRIVER_STATUS_LABELS: Record<DriverStatus, string> = {
  AVAILABLE: 'Disponible',
  ON_MISSION: 'En mission',
  UNAVAILABLE: 'Indisponible',
  SUSPENDED: 'Suspendu',
}

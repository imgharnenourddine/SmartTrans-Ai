export type VehicleStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'OUT_OF_SERVICE'

export type VehicleType = 'TRUCK' | 'VAN' | 'TRAILER' | 'OTHER'

export interface Vehicle {
  id: string
  registrationNumber: string
  type: VehicleType
  capacityKg: number | null
  mileageKm: number
  consumptionLPer100km: number | null
  insuranceExpiresAt: string | null
  technicalVisitExpiresAt: string | null
  status: VehicleStatus
  createdAt: string
  updatedAt: string
}

export interface CreateVehicleRequest {
  registrationNumber: string
  type: VehicleType
  capacityKg?: number | null
  mileageKm?: number | null
  consumptionLPer100km?: number | null
  insuranceExpiresAt?: string | null
  technicalVisitExpiresAt?: string | null
  status?: VehicleStatus
}

export interface UpdateVehicleRequest {
  registrationNumber?: string
  type?: VehicleType
  capacityKg?: number | null
  mileageKm?: number | null
  consumptionLPer100km?: number | null
  insuranceExpiresAt?: string | null
  technicalVisitExpiresAt?: string | null
  status?: VehicleStatus
}

export interface VehicleSearchParams {
  search?: string
  status?: VehicleStatus
  availableOnly?: boolean
  insuranceExpiringWithinDays?: number
  technicalVisitExpiringWithinDays?: number
}

export const VEHICLE_STATUS_LABELS: Record<VehicleStatus, string> = {
  AVAILABLE: 'Disponible',
  IN_USE: 'En utilisation',
  MAINTENANCE: 'En maintenance',
  OUT_OF_SERVICE: 'Hors service',
}

export const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  TRUCK: 'Camion',
  VAN: 'Fourgon',
  TRAILER: 'Remorque',
  OTHER: 'Autre',
}

export type DocumentState = 'UNKNOWN' | 'EXPIRED' | 'EXPIRING_SOON' | 'VALID'

export function documentState(expiresAt: string | null, thresholdDays = 30): DocumentState {
  if (!expiresAt) {
    return 'UNKNOWN'
  }
  const expiry = new Date(expiresAt)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const daysLeft = Math.round((expiry.getTime() - today.getTime()) / 86_400_000)
  if (daysLeft < 0) {
    return 'EXPIRED'
  }
  return daysLeft <= thresholdDays ? 'EXPIRING_SOON' : 'VALID'
}

export function daysUntil(expiresAt: string | null): number | null {
  if (!expiresAt) {
    return null
  }
  const expiry = new Date(expiresAt)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((expiry.getTime() - today.getTime()) / 86_400_000)
}

export function isAvailableForMission(vehicle: Vehicle): boolean {
  if (vehicle.status !== 'AVAILABLE') {
    return false
  }
  return (
    documentState(vehicle.insuranceExpiresAt) !== 'EXPIRED' &&
    documentState(vehicle.technicalVisitExpiresAt) !== 'EXPIRED'
  )
}

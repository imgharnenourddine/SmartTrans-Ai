import { apiClient } from '../api/client'
import type {
  CreateVehicleRequest,
  UpdateVehicleRequest,
  Vehicle,
  VehicleSearchParams,
} from '../types/vehicle'

function buildSearchParams(params?: VehicleSearchParams): URLSearchParams {
  const query = new URLSearchParams()
  if (!params) {
    return query
  }
  if (params.search?.trim()) {
    query.set('search', params.search.trim())
  }
  if (params.status) {
    query.set('status', params.status)
  }
  if (params.availableOnly !== undefined) {
    query.set('availableOnly', String(params.availableOnly))
  }
  if (params.insuranceExpiringWithinDays !== undefined) {
    query.set('insuranceExpiringWithinDays', String(params.insuranceExpiringWithinDays))
  }
  if (params.technicalVisitExpiringWithinDays !== undefined) {
    query.set('technicalVisitExpiringWithinDays', String(params.technicalVisitExpiringWithinDays))
  }
  return query
}

export const vehicleService = {
  async list(params?: VehicleSearchParams): Promise<Vehicle[]> {
    const query = buildSearchParams(params)
    const suffix = query.toString() ? `?${query.toString()}` : ''
    const { data } = await apiClient.get<Vehicle[]>(`/vehicles${suffix}`)
    return data
  },

  async getById(id: string): Promise<Vehicle> {
    const { data } = await apiClient.get<Vehicle>(`/vehicles/${id}`)
    return data
  },

  async getWithExpiringDocuments(withinDays = 30): Promise<Vehicle[]> {
    const { data } = await apiClient.get<Vehicle[]>(`/vehicles/expiring-documents?withinDays=${withinDays}`)
    return data
  },

  async create(payload: CreateVehicleRequest): Promise<Vehicle> {
    const { data } = await apiClient.post<Vehicle>('/vehicles', payload)
    return data
  },

  async update(id: string, payload: UpdateVehicleRequest): Promise<Vehicle> {
    const { data } = await apiClient.patch<Vehicle>(`/vehicles/${id}`, payload)
    return data
  },

  async deactivate(id: string): Promise<Vehicle> {
    const { data } = await apiClient.patch<Vehicle>(`/vehicles/${id}/deactivate`)
    return data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/vehicles/${id}`)
  },
}

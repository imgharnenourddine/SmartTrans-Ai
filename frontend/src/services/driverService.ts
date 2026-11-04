import { apiClient } from '../api/client'
import type {
  CreateDriverRequest,
  Driver,
  DriverSearchParams,
  UpdateDriverAvailabilityRequest,
  UpdateDriverRequest,
} from '../types/driver'

function buildSearchParams(params?: DriverSearchParams): URLSearchParams {
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
  if (params.licenseExpired !== undefined) {
    query.set('licenseExpired', String(params.licenseExpired))
  }
  if (params.availableOnly !== undefined) {
    query.set('availableOnly', String(params.availableOnly))
  }
  return query
}

export const driverService = {
  async list(params?: DriverSearchParams): Promise<Driver[]> {
    const query = buildSearchParams(params)
    const suffix = query.toString() ? `?${query.toString()}` : ''
    const { data } = await apiClient.get<Driver[]>(`/drivers${suffix}`)
    return data
  },

  async getById(id: string): Promise<Driver> {
    const { data } = await apiClient.get<Driver>(`/drivers/${id}`)
    return data
  },

  async getMe(): Promise<Driver> {
    const { data } = await apiClient.get<Driver>('/drivers/me')
    return data
  },

  async getExpiringLicenses(withinDays = 30): Promise<Driver[]> {
    const { data } = await apiClient.get<Driver[]>(`/drivers/expiring-licenses?withinDays=${withinDays}`)
    return data
  },

  async create(payload: CreateDriverRequest): Promise<Driver> {
    const { data } = await apiClient.post<Driver>('/drivers', payload)
    return data
  },

  async update(id: string, payload: UpdateDriverRequest): Promise<Driver> {
    const { data } = await apiClient.patch<Driver>(`/drivers/${id}`, payload)
    return data
  },

  async updateAvailability(id: string, payload: UpdateDriverAvailabilityRequest): Promise<Driver> {
    const { data } = await apiClient.patch<Driver>(`/drivers/${id}/availability`, payload)
    return data
  },

  async deactivate(id: string): Promise<Driver> {
    const { data } = await apiClient.patch<Driver>(`/drivers/${id}/deactivate`)
    return data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/drivers/${id}`)
  },
}

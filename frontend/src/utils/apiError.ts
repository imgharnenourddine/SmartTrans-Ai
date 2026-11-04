import type { AxiosError } from 'axios'
import type { ApiErrorResponse } from '../types/auth'

export function getApiErrorMessage(error: unknown, fallback = 'Une erreur est survenue'): string {
  const axiosError = error as AxiosError<ApiErrorResponse>
  const data = axiosError.response?.data

	if (data?.message) {
    return data.message
  }

  if (axiosError.response?.status === 403) {
    return 'Accès refusé. Ouvre l’app en http://127.0.0.1:5173 (évite un autre nom d’hôte).'
  }

  if (axiosError.message) {
    return axiosError.message
  }

  return fallback
}

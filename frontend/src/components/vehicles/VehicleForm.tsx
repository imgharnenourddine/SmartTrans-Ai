import { useState, type FormEvent } from 'react'
import type { CreateVehicleRequest, UpdateVehicleRequest, VehicleStatus, VehicleType } from '../../types/vehicle'
import { VEHICLE_STATUS_LABELS, VEHICLE_TYPE_LABELS } from '../../types/vehicle'

export type VehicleFormValues = {
  registrationNumber: string
  type: VehicleType
  capacityKg: string
  mileageKm: string
  consumptionLPer100km: string
  insuranceExpiresAt: string
  technicalVisitExpiresAt: string
  status: VehicleStatus
}

interface VehicleFormProps {
  initialValues?: Partial<VehicleFormValues>
  submitLabel: string
  loading?: boolean
  error?: string | null
  showStatus?: boolean
  onSubmit: (payload: CreateVehicleRequest | UpdateVehicleRequest) => Promise<void>
}

const DEFAULT_VALUES: VehicleFormValues = {
  registrationNumber: '',
  type: 'TRUCK',
  capacityKg: '',
  mileageKm: '',
  consumptionLPer100km: '',
  insuranceExpiresAt: '',
  technicalVisitExpiresAt: '',
  status: 'AVAILABLE',
}

export function VehicleForm({
  initialValues,
  submitLabel,
  loading = false,
  error = null,
  showStatus = true,
  onSubmit,
}: VehicleFormProps) {
  const [values, setValues] = useState<VehicleFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
  })

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const payload: CreateVehicleRequest | UpdateVehicleRequest = {
      registrationNumber: values.registrationNumber.trim(),
      type: values.type,
      capacityKg: values.capacityKg ? Number(values.capacityKg) : null,
      mileageKm: values.mileageKm ? Number(values.mileageKm) : 0,
      consumptionLPer100km: values.consumptionLPer100km ? Number(values.consumptionLPer100km) : null,
      insuranceExpiresAt: values.insuranceExpiresAt || null,
      technicalVisitExpiresAt: values.technicalVisitExpiresAt || null,
    }

    if (showStatus) {
      payload.status = values.status
    }

    await onSubmit(payload)
  }

  return (
    <form className="vehicle-form" onSubmit={handleSubmit}>
      {error && <p className="form-error">{error}</p>}

      <div className="form-grid">
        <label className="field">
          Immatriculation
          <input
            required
            maxLength={20}
            value={values.registrationNumber}
            onChange={(event) => setValues((current) => ({ ...current, registrationNumber: event.target.value }))}
            placeholder="12345-A-6"
          />
        </label>

        <label className="field">
          Type
          <select
            value={values.type}
            onChange={(event) => setValues((current) => ({ ...current, type: event.target.value as VehicleType }))}
          >
            {(Object.keys(VEHICLE_TYPE_LABELS) as VehicleType[]).map((type) => (
              <option key={type} value={type}>
                {VEHICLE_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Capacité (kg)
          <input
            type="number"
            min={0}
            step="0.01"
            value={values.capacityKg}
            onChange={(event) => setValues((current) => ({ ...current, capacityKg: event.target.value }))}
          />
        </label>

        <label className="field">
          Kilométrage (km)
          <input
            type="number"
            min={0}
            value={values.mileageKm}
            onChange={(event) => setValues((current) => ({ ...current, mileageKm: event.target.value }))}
          />
        </label>

        <label className="field">
          Consommation (L/100 km)
          <input
            type="number"
            min={0}
            step="0.01"
            value={values.consumptionLPer100km}
            onChange={(event) => setValues((current) => ({ ...current, consumptionLPer100km: event.target.value }))}
          />
        </label>

        <label className="field">
          Expiration de l&apos;assurance
          <input
            type="date"
            value={values.insuranceExpiresAt}
            onChange={(event) => setValues((current) => ({ ...current, insuranceExpiresAt: event.target.value }))}
          />
        </label>

        <label className="field">
          Expiration de la visite technique
          <input
            type="date"
            value={values.technicalVisitExpiresAt}
            onChange={(event) => setValues((current) => ({ ...current, technicalVisitExpiresAt: event.target.value }))}
          />
        </label>

        {showStatus && (
          <label className="field">
            Statut
            <select
              value={values.status}
              onChange={(event) =>
                setValues((current) => ({ ...current, status: event.target.value as VehicleStatus }))
              }
            >
              {(Object.keys(VEHICLE_STATUS_LABELS) as VehicleStatus[]).map((status) => (
                <option key={status} value={status}>
                  {VEHICLE_STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      <button type="submit" className="btn btn-primary btn-inline" disabled={loading}>
        {loading ? 'Enregistrement…' : submitLabel}
      </button>
    </form>
  )
}

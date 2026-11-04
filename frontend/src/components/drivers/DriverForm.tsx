import { useState, type FormEvent } from 'react'
import type { CreateDriverRequest, DriverStatus, UpdateDriverRequest } from '../../types/driver'
import { DRIVER_STATUS_LABELS } from '../../types/driver'

export type DriverFormValues = {
  firstName: string
  lastName: string
  phone: string
  licenseNumber: string
  licenseExpiresAt: string
  experienceYears: string
  status: DriverStatus
}

interface DriverFormProps {
  initialValues?: Partial<DriverFormValues>
  submitLabel: string
  loading?: boolean
  error?: string | null
  showStatus?: boolean
  onSubmit: (payload: CreateDriverRequest | UpdateDriverRequest) => Promise<void>
}

const DEFAULT_VALUES: DriverFormValues = {
  firstName: '',
  lastName: '',
  phone: '',
  licenseNumber: '',
  licenseExpiresAt: '',
  experienceYears: '',
  status: 'AVAILABLE',
}

export function DriverForm({
  initialValues,
  submitLabel,
  loading = false,
  error = null,
  showStatus = true,
  onSubmit,
}: DriverFormProps) {
  const [values, setValues] = useState<DriverFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
  })

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const payload: CreateDriverRequest | UpdateDriverRequest = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      phone: values.phone.trim(),
      licenseNumber: values.licenseNumber.trim(),
      licenseExpiresAt: values.licenseExpiresAt,
      experienceYears: values.experienceYears ? Number(values.experienceYears) : null,
    }

    if (showStatus) {
      payload.status = values.status
    }

    await onSubmit(payload)
  }

  return (
    <form className="driver-form" onSubmit={handleSubmit}>
      {error && <p className="form-error">{error}</p>}

      <div className="form-grid">
        <label className="field">
          Prénom
          <input
            required
            maxLength={100}
            value={values.firstName}
            onChange={(event) => setValues((current) => ({ ...current, firstName: event.target.value }))}
          />
        </label>

        <label className="field">
          Nom
          <input
            required
            maxLength={100}
            value={values.lastName}
            onChange={(event) => setValues((current) => ({ ...current, lastName: event.target.value }))}
          />
        </label>

        <label className="field">
          Téléphone
          <input
            required
            maxLength={30}
            value={values.phone}
            onChange={(event) => setValues((current) => ({ ...current, phone: event.target.value }))}
          />
        </label>

        <label className="field">
          Numéro de permis
          <input
            required
            maxLength={50}
            value={values.licenseNumber}
            onChange={(event) => setValues((current) => ({ ...current, licenseNumber: event.target.value }))}
          />
        </label>

        <label className="field">
          Expiration du permis
          <input
            required
            type="date"
            value={values.licenseExpiresAt}
            onChange={(event) => setValues((current) => ({ ...current, licenseExpiresAt: event.target.value }))}
          />
        </label>

        <label className="field">
          Années d&apos;expérience
          <input
            type="number"
            min={0}
            value={values.experienceYears}
            onChange={(event) => setValues((current) => ({ ...current, experienceYears: event.target.value }))}
          />
        </label>

        {showStatus && (
          <label className="field">
            Statut
            <select
              value={values.status}
              onChange={(event) =>
                setValues((current) => ({ ...current, status: event.target.value as DriverStatus }))
              }
            >
              {(Object.keys(DRIVER_STATUS_LABELS) as DriverStatus[]).map((status) => (
                <option key={status} value={status}>
                  {DRIVER_STATUS_LABELS[status]}
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

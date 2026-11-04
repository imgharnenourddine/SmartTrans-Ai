import { useEffect, useState, type FormEvent } from 'react'
import { clientService } from '../../services/clientService'
import { driverService } from '../../services/driverService'
import { vehicleService } from '../../services/vehicleService'
import type { Client } from '../../types/client'
import type { Driver } from '../../types/driver'
import type { CreateMissionRequest, UpdateMissionRequest } from '../../types/mission'
import { fromDatetimeLocalValue } from '../../types/mission'
import type { Vehicle } from '../../types/vehicle'
import { getApiErrorMessage } from '../../utils/apiError'

export type MissionFormValues = {
  reference: string
  clientId: string
  driverId: string
  vehicleId: string
  departureLocation: string
  destination: string
  scheduledAt: string
  weightKg: string
  cargoType: string
  observations: string
}

interface MissionFormProps {
  initialValues?: Partial<MissionFormValues>
  submitLabel: string
  loading?: boolean
  error?: string | null
  lockAssignments?: boolean
  onSubmit: (payload: CreateMissionRequest | UpdateMissionRequest) => Promise<void>
}

const DEFAULT_VALUES: MissionFormValues = {
  reference: '',
  clientId: '',
  driverId: '',
  vehicleId: '',
  departureLocation: '',
  destination: '',
  scheduledAt: '',
  weightKg: '',
  cargoType: '',
  observations: '',
}

export function MissionForm({
  initialValues,
  submitLabel,
  loading = false,
  error = null,
  lockAssignments = false,
  onSubmit,
}: MissionFormProps) {
  const [values, setValues] = useState<MissionFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
  })
  const [clients, setClients] = useState<Client[]>([])
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [optionsError, setOptionsError] = useState<string | null>(null)
  const [optionsLoading, setOptionsLoading] = useState(true)

  useEffect(() => {
    async function loadOptions() {
      setOptionsLoading(true)
      setOptionsError(null)
      try {
        const [clientData, driverData, vehicleData] = await Promise.all([
          clientService.list(),
          driverService.list(),
          vehicleService.list(),
        ])
        setClients(clientData)
        setDrivers(driverData)
        setVehicles(vehicleData)
      } catch (err) {
        setOptionsError(getApiErrorMessage(err, 'Impossible de charger les listes de sélection'))
      } finally {
        setOptionsLoading(false)
      }
    }

    void loadOptions()
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const payload: CreateMissionRequest | UpdateMissionRequest = {
      reference: values.reference.trim(),
      clientId: values.clientId,
      departureLocation: values.departureLocation.trim(),
      destination: values.destination.trim(),
      scheduledAt: fromDatetimeLocalValue(values.scheduledAt),
      cargoType: values.cargoType.trim() || null,
      observations: values.observations.trim() || null,
      weightKg: values.weightKg ? Number(values.weightKg) : null,
    }

    if (!lockAssignments) {
      payload.driverId = values.driverId || null
      payload.vehicleId = values.vehicleId || null
    }

    await onSubmit(payload)
  }

  return (
    <form className="mission-form" onSubmit={handleSubmit}>
      {(error || optionsError) && <p className="form-error">{error ?? optionsError}</p>}

      {optionsLoading && <p className="page-loading">Chargement des listes…</p>}

      <div className="form-grid">
        <label className="field">
          Référence
          <input
            required
            maxLength={30}
            value={values.reference}
            onChange={(event) => setValues((current) => ({ ...current, reference: event.target.value }))}
          />
        </label>

        <label className="field">
          Client
          <select
            required
            value={values.clientId}
            onChange={(event) => setValues((current) => ({ ...current, clientId: event.target.value }))}
          >
            <option value="">Sélectionner un client</option>
            {clients.map((client) => (
              <option key={client.id} value={client.id}>
                {client.companyName}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Départ
          <input
            required
            maxLength={255}
            value={values.departureLocation}
            onChange={(event) => setValues((current) => ({ ...current, departureLocation: event.target.value }))}
          />
        </label>

        <label className="field">
          Destination
          <input
            required
            maxLength={255}
            value={values.destination}
            onChange={(event) => setValues((current) => ({ ...current, destination: event.target.value }))}
          />
        </label>

        <label className="field">
          Date planifiée
          <input
            required
            type="datetime-local"
            value={values.scheduledAt}
            onChange={(event) => setValues((current) => ({ ...current, scheduledAt: event.target.value }))}
          />
        </label>

        <label className="field">
          Poids (kg)
          <input
            type="number"
            min={0.01}
            step="0.01"
            value={values.weightKg}
            onChange={(event) => setValues((current) => ({ ...current, weightKg: event.target.value }))}
          />
        </label>

        <label className="field">
          Type de marchandise
          <input
            maxLength={100}
            value={values.cargoType}
            onChange={(event) => setValues((current) => ({ ...current, cargoType: event.target.value }))}
          />
        </label>

        <label className="field">
          Chauffeur
          <select
            value={values.driverId}
            disabled={lockAssignments}
            onChange={(event) => setValues((current) => ({ ...current, driverId: event.target.value }))}
          >
            <option value="">Non assigné</option>
            {drivers.map((driver) => (
              <option key={driver.id} value={driver.id}>
                {driver.firstName} {driver.lastName}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Véhicule
          <select
            value={values.vehicleId}
            disabled={lockAssignments}
            onChange={(event) => setValues((current) => ({ ...current, vehicleId: event.target.value }))}
          >
            <option value="">Non assigné</option>
            {vehicles.map((vehicle) => (
              <option key={vehicle.id} value={vehicle.id}>
                {vehicle.registrationNumber}
              </option>
            ))}
          </select>
        </label>
      </div>

      {lockAssignments && (
        <p className="page-loading">Le chauffeur et le véhicule ne peuvent être réassignés que sur une mission planifiée.</p>
      )}

      <label className="field">
        Observations
        <textarea
          rows={3}
          value={values.observations}
          onChange={(event) => setValues((current) => ({ ...current, observations: event.target.value }))}
        />
      </label>

      <button type="submit" className="btn btn-primary btn-inline" disabled={loading || optionsLoading || Boolean(optionsError)}>
        {loading ? 'Enregistrement…' : submitLabel}
      </button>
    </form>
  )
}

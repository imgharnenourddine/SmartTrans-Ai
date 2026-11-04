import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { VehicleForm } from '../../components/vehicles/VehicleForm'
import { vehicleService } from '../../services/vehicleService'
import type { UpdateVehicleRequest, Vehicle } from '../../types/vehicle'
import { getApiErrorMessage } from '../../utils/apiError'

export function VehicleEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadVehicle() {
      setLoading(true)
      setError(null)
      try {
        const data = await vehicleService.getById(id!)
        setVehicle(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger le véhicule'))
      } finally {
        setLoading(false)
      }
    }

    void loadVehicle()
  }, [id])

  async function handleSubmit(payload: UpdateVehicleRequest) {
    if (!id) {
      return
    }

    setSaving(true)
    setError(null)
    try {
      const updated = await vehicleService.update(id, payload)
      navigate(`/vehicles/${updated.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour le véhicule'))
      setSaving(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement du véhicule…</p>
  }

  if (!vehicle) {
    return (
      <section className="vehicles-page">
        <p className="form-error">{error ?? 'Véhicule introuvable'}</p>
        <Link to="/vehicles">Retour à la liste</Link>
      </section>
    )
  }

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Modifier {vehicle.registrationNumber}</h2>
        <p>
          <Link to={`/vehicles/${vehicle.id}`}>← Retour au détail</Link>
        </p>
      </header>

      <article className="info-block">
        <VehicleForm
          submitLabel="Enregistrer les modifications"
          loading={saving}
          error={error}
          initialValues={{
            registrationNumber: vehicle.registrationNumber,
            type: vehicle.type,
            capacityKg: vehicle.capacityKg !== null ? String(vehicle.capacityKg) : '',
            mileageKm: String(vehicle.mileageKm),
            consumptionLPer100km: vehicle.consumptionLPer100km !== null ? String(vehicle.consumptionLPer100km) : '',
            insuranceExpiresAt: vehicle.insuranceExpiresAt ?? '',
            technicalVisitExpiresAt: vehicle.technicalVisitExpiresAt ?? '',
            status: vehicle.status,
          }}
          onSubmit={handleSubmit}
        />
      </article>
    </section>
  )
}

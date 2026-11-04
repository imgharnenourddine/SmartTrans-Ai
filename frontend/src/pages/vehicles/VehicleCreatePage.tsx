import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { VehicleForm } from '../../components/vehicles/VehicleForm'
import { vehicleService } from '../../services/vehicleService'
import type { CreateVehicleRequest, UpdateVehicleRequest } from '../../types/vehicle'
import { getApiErrorMessage } from '../../utils/apiError'

export function VehicleCreatePage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(payload: CreateVehicleRequest | UpdateVehicleRequest) {
    setLoading(true)
    setError(null)
    try {
      const created = await vehicleService.create(payload as CreateVehicleRequest)
      navigate(`/vehicles/${created.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer le véhicule'))
      setLoading(false)
    }
  }

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Nouveau véhicule</h2>
        <p>
          <Link to="/vehicles">← Retour à la liste</Link>
        </p>
      </header>

      <article className="info-block">
        <VehicleForm submitLabel="Créer le véhicule" loading={loading} error={error} onSubmit={handleSubmit} />
      </article>
    </section>
  )
}

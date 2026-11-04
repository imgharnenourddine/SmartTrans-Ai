import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { VehicleDocumentsInfo } from '../../components/vehicles/VehicleDocumentsInfo'
import { VehicleStatusBadge } from '../../components/vehicles/VehicleStatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { vehicleService } from '../../services/vehicleService'
import type { Vehicle } from '../../types/vehicle'
import { VEHICLE_TYPE_LABELS, isAvailableForMission } from '../../types/vehicle'
import { getApiErrorMessage } from '../../utils/apiError'

export function VehicleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'TRANSPORT_MANAGER')

  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
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

  async function handleDeactivate() {
    if (!vehicle || !window.confirm('Désactiver ce véhicule ?')) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      const updated = await vehicleService.deactivate(vehicle.id)
      setVehicle(updated)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de désactiver le véhicule'))
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement du véhicule…</p>
  }

  if (!vehicle) {
    return (
      <section className="vehicles-page">
        <p className="form-error">{error ?? 'Véhicule introuvable'}</p>
        {canManage && <Link to="/vehicles">Retour à la liste</Link>}
      </section>
    )
  }

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>{vehicle.registrationNumber}</h2>
          <p>
            {canManage && <Link to="/vehicles">← Retour à la liste</Link>}
          </p>
        </div>
        <div className="detail-actions">
          {canManage && vehicle.status !== 'OUT_OF_SERVICE' && (
            <>
              <Link to={`/vehicles/${vehicle.id}/edit`} className="btn btn-secondary btn-inline">
                Modifier
              </Link>
              <button
                type="button"
                className="btn btn-danger btn-inline"
                disabled={actionLoading || vehicle.status === 'IN_USE'}
                title={vehicle.status === 'IN_USE' ? 'Véhicule en utilisation sur une mission' : undefined}
                onClick={() => void handleDeactivate()}
              >
                Désactiver
              </button>
            </>
          )}
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <div className="dashboard-grid">
        <article className="info-block">
          <h3>Informations générales</h3>
          <dl className="meta-list">
            <div>
              <dt>Statut</dt>
              <dd>
                <VehicleStatusBadge status={vehicle.status} />
              </dd>
            </div>
            <div>
              <dt>Type</dt>
              <dd>{VEHICLE_TYPE_LABELS[vehicle.type]}</dd>
            </div>
            <div>
              <dt>Capacité</dt>
              <dd>
                {vehicle.capacityKg !== null ? `${vehicle.capacityKg.toLocaleString('fr-FR')} kg` : '—'}
              </dd>
            </div>
            <div>
              <dt>Kilométrage</dt>
              <dd>{vehicle.mileageKm.toLocaleString('fr-FR')} km</dd>
            </div>
            <div>
              <dt>Consommation</dt>
              <dd>
                {vehicle.consumptionLPer100km !== null ? `${vehicle.consumptionLPer100km} L/100 km` : '—'}
              </dd>
            </div>
            <div>
              <dt>Disponible pour mission</dt>
              <dd>{isAvailableForMission(vehicle) ? 'Oui' : 'Non'}</dd>
            </div>
          </dl>
        </article>

        <article className="info-block">
          <h3>Assurance et visite technique</h3>
          <VehicleDocumentsInfo vehicle={vehicle} />
        </article>
      </div>

      {canManage && (
        <button type="button" className="btn btn-ghost btn-inline back-link" onClick={() => navigate('/vehicles')}>
          Retour à la liste
        </button>
      )}
    </section>
  )
}

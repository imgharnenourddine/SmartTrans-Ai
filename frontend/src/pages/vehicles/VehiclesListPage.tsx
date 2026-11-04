import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { VehicleStatusBadge } from '../../components/vehicles/VehicleStatusBadge'
import { vehicleService } from '../../services/vehicleService'
import type { Vehicle, VehicleSearchParams, VehicleStatus } from '../../types/vehicle'
import { VEHICLE_STATUS_LABELS, VEHICLE_TYPE_LABELS, documentState, isAvailableForMission } from '../../types/vehicle'
import { getApiErrorMessage } from '../../utils/apiError'

const STATUS_OPTIONS: Array<{ value: '' | VehicleStatus; label: string }> = [
  { value: '', label: 'Tous les statuts' },
  ...((Object.keys(VEHICLE_STATUS_LABELS) as VehicleStatus[]).map((status) => ({
    value: status,
    label: VEHICLE_STATUS_LABELS[status],
  }))),
]

type DocumentsFilter = 'all' | 'insurance' | 'technicalVisit' | 'expiring'

export function VehiclesListPage() {
  const [searchParams] = useSearchParams()
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<'' | VehicleStatus>('')
  const [availableOnly, setAvailableOnly] = useState(false)
  const [documents, setDocuments] = useState<DocumentsFilter>(
    searchParams.get('alert') === 'documents' ? 'expiring' : 'all',
  )

  const loadVehicles = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      let data: Vehicle[]
      if (documents === 'expiring') {
        data = await vehicleService.getWithExpiringDocuments(30)
        const query = search.trim().toLowerCase()
        if (query) {
          data = data.filter((vehicle) => vehicle.registrationNumber.toLowerCase().includes(query))
        }
        if (status) {
          data = data.filter((vehicle) => vehicle.status === status)
        }
      } else {
        const params: VehicleSearchParams = {
          search: search || undefined,
          status: status || undefined,
          availableOnly: availableOnly || undefined,
          insuranceExpiringWithinDays: documents === 'insurance' ? 30 : undefined,
          technicalVisitExpiringWithinDays: documents === 'technicalVisit' ? 30 : undefined,
        }
        data = await vehicleService.list(params)
      }
      setVehicles(data)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les véhicules'))
    } finally {
      setLoading(false)
    }
  }, [availableOnly, documents, search, status])

  useEffect(() => {
    void loadVehicles()
  }, [loadVehicles])

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>Véhicules</h2>
          <p>Gestion de la flotte, assurances et visites techniques.</p>
        </div>
        <Link to="/vehicles/new" className="btn btn-primary btn-inline">
          Ajouter un véhicule
        </Link>
      </header>

      <div className="filters-panel">
        <label className="field">
          Recherche
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Immatriculation…"
          />
        </label>

        <label className="field">
          Statut
          <select value={status} onChange={(event) => setStatus(event.target.value as '' | VehicleStatus)}>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.label} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Documents
          <select
            value={documents}
            onChange={(event) => setDocuments(event.target.value as DocumentsFilter)}
          >
            <option value="all">Tous</option>
            <option value="expiring">Assurance ou VT ≤ 30 jours</option>
            <option value="insurance">Assurance ≤ 30 jours</option>
            <option value="technicalVisit">Visite technique ≤ 30 jours</option>
          </select>
        </label>

        <label className="field checkbox-field">
          <span>Disponibles uniquement</span>
          <input
            type="checkbox"
            checked={availableOnly}
            onChange={(event) => setAvailableOnly(event.target.checked)}
          />
        </label>

        <button type="button" className="btn btn-secondary btn-inline" onClick={() => void loadVehicles()}>
          Actualiser
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des véhicules…</p>
      ) : vehicles.length === 0 ? (
        <div className="empty-state">Aucun véhicule ne correspond aux critères.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Immatriculation</th>
                <th>Type</th>
                <th>Capacité (kg)</th>
                <th>Kilométrage</th>
                <th>Assurance</th>
                <th>Visite technique</th>
                <th>Statut</th>
                <th>Disponible</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {vehicles.map((vehicle) => (
                <tr key={vehicle.id}>
                  <td>
                    <strong>{vehicle.registrationNumber}</strong>
                  </td>
                  <td>{VEHICLE_TYPE_LABELS[vehicle.type]}</td>
                  <td>{vehicle.capacityKg !== null ? vehicle.capacityKg.toLocaleString('fr-FR') : '—'}</td>
                  <td>{vehicle.mileageKm.toLocaleString('fr-FR')} km</td>
                  <td>
                    <DocumentCell expiresAt={vehicle.insuranceExpiresAt} />
                  </td>
                  <td>
                    <DocumentCell expiresAt={vehicle.technicalVisitExpiresAt} />
                  </td>
                  <td>
                    <VehicleStatusBadge status={vehicle.status} />
                  </td>
                  <td>{isAvailableForMission(vehicle) ? 'Oui' : 'Non'}</td>
                  <td className="table-actions">
                    <Link to={`/vehicles/${vehicle.id}`}>Voir</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function DocumentCell({ expiresAt }: { expiresAt: string | null }) {
  if (!expiresAt) {
    return <span>—</span>
  }
  const state = documentState(expiresAt)
  return (
    <>
      {new Date(expiresAt).toLocaleDateString('fr-FR')}{' '}
      {state === 'EXPIRED' && <span className="inline-tag tag-danger">Expirée</span>}
      {state === 'EXPIRING_SOON' && <span className="inline-tag tag-warning">Bientôt</span>}
    </>
  )
}

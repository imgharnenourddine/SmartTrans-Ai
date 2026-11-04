import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { DriverStatusBadge } from '../../components/drivers/DriverStatusBadge'
import { driverService } from '../../services/driverService'
import type { Driver, DriverSearchParams, DriverStatus } from '../../types/driver'
import { DRIVER_STATUS_LABELS } from '../../types/driver'
import { getApiErrorMessage } from '../../utils/apiError'

const STATUS_OPTIONS: Array<{ value: '' | DriverStatus; label: string }> = [
  { value: '', label: 'Tous les statuts' },
  ...((Object.keys(DRIVER_STATUS_LABELS) as DriverStatus[]).map((status) => ({
    value: status,
    label: DRIVER_STATUS_LABELS[status],
  }))),
]

export function DriversListPage() {
  const [searchParams] = useSearchParams()
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<'' | DriverStatus>('')
  const [availableOnly, setAvailableOnly] = useState(false)
  const [licenseFilter, setLicenseFilter] = useState<'all' | 'valid' | 'expired' | 'renew'>(
    searchParams.get('alert') === 'license' ? 'renew' : 'all',
  )

  const loadDrivers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      let data: Driver[]
      if (licenseFilter === 'renew') {
        data = await driverService.getExpiringLicenses(30)
        const query = search.trim().toLowerCase()
        if (query) {
          data = data.filter((driver) =>
            `${driver.firstName} ${driver.lastName} ${driver.phone} ${driver.licenseNumber}`
              .toLowerCase()
              .includes(query),
          )
        }
        if (status) {
          data = data.filter((driver) => driver.status === status)
        }
      } else {
        const params: DriverSearchParams = {
          search: search || undefined,
          status: status || undefined,
          availableOnly: availableOnly || undefined,
          licenseExpired: licenseFilter === 'all' ? undefined : licenseFilter === 'expired',
        }
        data = await driverService.list(params)
      }
      setDrivers(data)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les chauffeurs'))
    } finally {
      setLoading(false)
    }
  }, [availableOnly, licenseFilter, search, status])

  useEffect(() => {
    void loadDrivers()
  }, [loadDrivers])

  return (
    <section className="drivers-page">
      <header className="page-header page-header-row">
        <div>
          <h2>Chauffeurs</h2>
          <p>Gestion de la flotte humaine et suivi des permis.</p>
        </div>
        <Link to="/drivers/new" className="btn btn-primary btn-inline">
          Ajouter un chauffeur
        </Link>
      </header>

      <div className="filters-panel">
        <label className="field">
          Recherche
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Nom, téléphone, permis…"
          />
        </label>

        <label className="field">
          Statut
          <select value={status} onChange={(event) => setStatus(event.target.value as '' | DriverStatus)}>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.label} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Permis
          <select
            value={licenseFilter}
            onChange={(event) => setLicenseFilter(event.target.value as 'all' | 'valid' | 'expired' | 'renew')}
          >
            <option value="all">Tous</option>
            <option value="valid">Valides</option>
            <option value="expired">Expirés</option>
            <option value="renew">À renouveler (≤ 30 jours)</option>
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

        <button type="button" className="btn btn-secondary btn-inline" onClick={() => void loadDrivers()}>
          Actualiser
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des chauffeurs…</p>
      ) : drivers.length === 0 ? (
        <div className="empty-state">Aucun chauffeur ne correspond aux critères.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Chauffeur</th>
                <th>Téléphone</th>
                <th>Permis</th>
                <th>Expiration</th>
                <th>Statut</th>
                <th>Disponible</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {drivers.map((driver) => (
                <tr key={driver.id}>
                  <td>
                    <strong>
                      {driver.firstName} {driver.lastName}
                    </strong>
                  </td>
                  <td>{driver.phone}</td>
                  <td>{driver.licenseNumber}</td>
                  <td>
                    {new Date(driver.licenseExpiresAt).toLocaleDateString('fr-FR')}
                    {driver.licenseExpired && <span className="inline-tag tag-danger">Expiré</span>}
                    {!driver.licenseExpired && driver.licenseExpiringSoon && (
                      <span className="inline-tag tag-warning">Bientôt</span>
                    )}
                  </td>
                  <td>
                    <DriverStatusBadge status={driver.status} />
                  </td>
                  <td>{driver.availableForMission ? 'Oui' : 'Non'}</td>
                  <td className="table-actions">
                    <Link to={`/drivers/${driver.id}`}>Voir</Link>
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

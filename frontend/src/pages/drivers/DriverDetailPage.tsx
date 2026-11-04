import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { DriverLicenseInfo } from '../../components/drivers/DriverLicenseInfo'
import { DriverStatusBadge } from '../../components/drivers/DriverStatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { driverService } from '../../services/driverService'
import type { Driver } from '../../types/driver'
import { getApiErrorMessage } from '../../utils/apiError'

export function DriverDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'TRANSPORT_MANAGER')

  const [driver, setDriver] = useState<Driver | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadDriver() {
      setLoading(true)
      setError(null)
      try {
        const data = await driverService.getById(id!)
        setDriver(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger le chauffeur'))
      } finally {
        setLoading(false)
      }
    }

    void loadDriver()
  }, [id])

  async function handleAvailability(nextStatus: 'AVAILABLE' | 'UNAVAILABLE') {
    if (!driver) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      const updated = await driverService.updateAvailability(driver.id, { status: nextStatus })
      setDriver(updated)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour la disponibilité'))
    } finally {
      setActionLoading(false)
    }
  }

  async function handleDeactivate() {
    if (!driver || !window.confirm('Désactiver ce chauffeur ?')) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      const updated = await driverService.deactivate(driver.id)
      setDriver(updated)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de désactiver le chauffeur'))
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement du chauffeur…</p>
  }

  if (!driver) {
    return (
      <section className="drivers-page">
        <p className="form-error">{error ?? 'Chauffeur introuvable'}</p>
        {canManage && <Link to="/drivers">Retour à la liste</Link>}
      </section>
    )
  }

  return (
    <section className="drivers-page">
      <header className="page-header page-header-row">
        <div>
          <h2>
            {driver.firstName} {driver.lastName}
          </h2>
          <p>
            {canManage && <Link to="/drivers">← Retour à la liste</Link>}
          </p>
        </div>
        <div className="detail-actions">
          {canManage && driver.status !== 'SUSPENDED' && (
            <>
              <Link to={`/drivers/${driver.id}/edit`} className="btn btn-secondary btn-inline">
                Modifier
              </Link>
              <button
                type="button"
                className="btn btn-secondary btn-inline"
                disabled={actionLoading || driver.status === 'ON_MISSION'}
                onClick={() => void handleAvailability('AVAILABLE')}
              >
                Disponible
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-inline"
                disabled={actionLoading || driver.status === 'ON_MISSION'}
                onClick={() => void handleAvailability('UNAVAILABLE')}
              >
                Indisponible
              </button>
              <button
                type="button"
                className="btn btn-danger btn-inline"
                disabled={actionLoading || driver.status === 'ON_MISSION'}
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
                <DriverStatusBadge status={driver.status} />
              </dd>
            </div>
            <div>
              <dt>Téléphone</dt>
              <dd>{driver.phone}</dd>
            </div>
            <div>
              <dt>Expérience</dt>
              <dd>{driver.experienceYears ?? '—'} ans</dd>
            </div>
            <div>
              <dt>Disponible pour mission</dt>
              <dd>{driver.availableForMission ? 'Oui' : 'Non'}</dd>
            </div>
          </dl>
        </article>

        <article className="info-block">
          <h3>Permis de conduire</h3>
          <DriverLicenseInfo driver={driver} />
        </article>
      </div>

      {canManage && (
        <button type="button" className="btn btn-ghost btn-inline back-link" onClick={() => navigate('/drivers')}>
          Retour à la liste
        </button>
      )}
    </section>
  )
}

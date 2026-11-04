import { useEffect, useState } from 'react'
import { DriverLicenseInfo } from '../../components/drivers/DriverLicenseInfo'
import { DriverStatusBadge } from '../../components/drivers/DriverStatusBadge'
import { driverService } from '../../services/driverService'
import type { Driver } from '../../types/driver'
import { getApiErrorMessage } from '../../utils/apiError'

export function DriverProfilePage() {
  const [driver, setDriver] = useState<Driver | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function loadProfile() {
      setLoading(true)
      setError(null)
      try {
        const data = await driverService.getMe()
        setDriver(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Profil chauffeur introuvable'))
      } finally {
        setLoading(false)
      }
    }

    void loadProfile()
  }, [])

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
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour votre disponibilité'))
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement de votre profil…</p>
  }

  if (!driver) {
    return (
      <section className="drivers-page">
        <header className="page-header">
          <h2>Mon profil chauffeur</h2>
        </header>
        <p className="form-error">{error ?? 'Aucun profil chauffeur associé à votre compte.'}</p>
      </section>
    )
  }

  return (
    <section className="drivers-page">
      <header className="page-header page-header-row">
        <div>
          <h2>Mon profil chauffeur</h2>
          <p>Gérez votre disponibilité et consultez votre permis.</p>
        </div>
        {driver.status !== 'ON_MISSION' && driver.status !== 'SUSPENDED' && (
          <div className="detail-actions">
            <button
              type="button"
              className="btn btn-secondary btn-inline"
              disabled={actionLoading || driver.licenseExpired}
              onClick={() => void handleAvailability('AVAILABLE')}
            >
              Me déclarer disponible
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-inline"
              disabled={actionLoading}
              onClick={() => void handleAvailability('UNAVAILABLE')}
            >
              Me déclarer indisponible
            </button>
          </div>
        )}
      </header>

      {error && <p className="form-error">{error}</p>}

      <div className="dashboard-grid">
        <article className="info-block">
          <h3>Informations</h3>
          <dl className="meta-list">
            <div>
              <dt>Nom</dt>
              <dd>
                {driver.firstName} {driver.lastName}
              </dd>
            </div>
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
    </section>
  )
}

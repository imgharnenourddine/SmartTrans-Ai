import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { MissionStatusChart } from '../components/dashboard/MissionStatusChart'
import { MissionWeekChart } from '../components/dashboard/MissionWeekChart'
import { DriverStatusBadge } from '../components/drivers/DriverStatusBadge'
import { MissionStatusBadge } from '../components/missions/MissionStatusBadge'
import { useAuth } from '../hooks/useAuth'
import { clientService } from '../services/clientService'
import { driverService } from '../services/driverService'
import { missionService } from '../services/missionService'
import { vehicleService } from '../services/vehicleService'
import type { Driver } from '../types/driver'
import type { Mission } from '../types/mission'
import { MISSION_STATUS_LABELS } from '../types/mission'
import type { Vehicle } from '../types/vehicle'
import { documentState, isAvailableForMission } from '../types/vehicle'
import { getApiErrorMessage } from '../utils/apiError'

interface ManagerStats {
  missions: Mission[]
  drivers: Driver[]
  vehicles: Vehicle[]
  clients: number
  missionsPlanified: number
  missionsInProgress: number
  missionsCompleted: number
  driversAvailable: number
  vehiclesAvailable: number
  expiringLicenses: Driver[]
  expiringVehicles: Vehicle[]
}

interface DriverStats {
  missions: Mission[]
  missionsPlanified: number
  missionsInProgress: number
  profile: Driver | null
}

function countByStatus(missions: Mission[], status: Mission['status']): number {
  return missions.filter((mission) => mission.status === status).length
}

function StatCard({
  label,
  value,
  hint,
  to,
  featured = false,
  warning = false,
}: {
  label: string
  value: number | string
  hint?: string
  to?: string
  featured?: boolean
  warning?: boolean
}) {
  const className = `kpi-card${featured ? ' kpi-featured' : ''}${warning ? ' kpi-warning' : ''}`
  const body = (
    <>
      <p className="kpi-label">{label}</p>
      <p className="kpi-value">{value}</p>
      {hint && <p className="kpi-hint">{hint}</p>}
    </>
  )
  if (to) {
    return (
      <Link className={className} to={to}>
        {body}
      </Link>
    )
  }
  return <article className={className}>{body}</article>
}

function vehicleAlertLabel(vehicle: Vehicle): string {
  const insurance = documentState(vehicle.insuranceExpiresAt)
  const technical = documentState(vehicle.technicalVisitExpiresAt)
  const parts: string[] = []
  if (insurance === 'EXPIRED' || insurance === 'EXPIRING_SOON') {
    parts.push('assurance')
  }
  if (technical === 'EXPIRED' || technical === 'EXPIRING_SOON') {
    parts.push('visite technique')
  }
  return parts.join(' · ') || 'document'
}

function upcomingMissions(missions: Mission[]): Mission[] {
  return [...missions]
    .filter((mission) => mission.status === 'PLANIFIED' || mission.status === 'IN_PROGRESS')
    .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime())
    .slice(0, 6)
}

export function DashboardPage() {
  const { user, hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'TRANSPORT_MANAGER')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [managerStats, setManagerStats] = useState<ManagerStats | null>(null)
  const [driverStats, setDriverStats] = useState<DriverStats | null>(null)

  useEffect(() => {
    async function loadDashboard() {
      setLoading(true)
      setError(null)
      try {
        if (canManage) {
          const [missions, drivers, vehicles, clients, expiringLicenses, expiringVehicles] =
            await Promise.all([
              missionService.list(),
              driverService.list(),
              vehicleService.list(),
              clientService.list(),
              driverService.getExpiringLicenses(30),
              vehicleService.getWithExpiringDocuments(30),
            ])
          setManagerStats({
            missions,
            drivers,
            vehicles,
            clients: clients.length,
            missionsPlanified: countByStatus(missions, 'PLANIFIED'),
            missionsInProgress: countByStatus(missions, 'IN_PROGRESS'),
            missionsCompleted: countByStatus(missions, 'COMPLETED'),
            driversAvailable: drivers.filter((driver) => driver.availableForMission).length,
            vehiclesAvailable: vehicles.filter((vehicle) => isAvailableForMission(vehicle)).length,
            expiringLicenses,
            expiringVehicles,
          })
          return
        }

        const [missionsResult, profileResult] = await Promise.allSettled([
          missionService.list(),
          driverService.getMe(),
        ])
        const missions = missionsResult.status === 'fulfilled' ? missionsResult.value : []
        if (missionsResult.status === 'rejected') {
          throw missionsResult.reason
        }
        setDriverStats({
          missions,
          missionsPlanified: countByStatus(missions, 'PLANIFIED'),
          missionsInProgress: countByStatus(missions, 'IN_PROGRESS'),
          profile: profileResult.status === 'fulfilled' ? profileResult.value : null,
        })
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger le tableau de bord'))
      } finally {
        setLoading(false)
      }
    }

    void loadDashboard()
  }, [canManage])

  const licenseWarning = Boolean(
    driverStats?.profile && (driverStats.profile.licenseExpired || driverStats.profile.licenseExpiringSoon),
  )

  const nextMissions = useMemo(
    () => (canManage && managerStats ? upcomingMissions(managerStats.missions) : driverStats ? upcomingMissions(driverStats.missions) : []),
    [canManage, managerStats, driverStats],
  )

  const driversOnMission = managerStats?.drivers.filter((driver) => driver.status === 'ON_MISSION') ?? []

  return (
    <section className="dashboard">
      <header className="page-header page-header-row">
        <div>
          <h2>Dashboard</h2>
          <p>Bonjour {user?.firstName}, voici l’activité transport en un coup d’œil.</p>
        </div>
        {canManage && (
          <div className="detail-actions">
            <Link to="/missions/new" className="btn btn-primary btn-inline">
              + Nouvelle mission
            </Link>
            <Link to="/missions" className="btn btn-secondary btn-inline">
              Voir les missions
            </Link>
          </div>
        )}
      </header>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des indicateurs…</p>
      ) : canManage && managerStats ? (
        <>
          <div className="kpi-grid kpi-grid-featured">
            <StatCard
              label="Missions"
              value={managerStats.missions.length}
              hint={`${managerStats.missionsPlanified} planifiées · ${managerStats.missionsInProgress} en cours`}
              to="/missions"
              featured
            />
            <StatCard
              label="Terminées"
              value={managerStats.missionsCompleted}
              hint="Missions clôturées"
              to="/missions"
            />
            <StatCard
              label="Chauffeurs dispo"
              value={managerStats.driversAvailable}
              hint={`sur ${managerStats.drivers.length} au total`}
              to="/drivers"
            />
            <StatCard
              label="Véhicules dispo"
              value={managerStats.vehiclesAvailable}
              hint={`sur ${managerStats.vehicles.length} au total`}
              to="/vehicles"
            />
            <StatCard
              label="Permis ≤ 30 jours"
              value={managerStats.expiringLicenses.length}
              hint="À renouveler"
              to="/drivers?alert=license"
              warning={managerStats.expiringLicenses.length > 0}
            />
            <StatCard
              label="Assurance / VT"
              value={managerStats.expiringVehicles.length}
              hint="Échéances véhicules"
              to="/vehicles?alert=documents"
              warning={managerStats.expiringVehicles.length > 0}
            />
          </div>

          <div className="dash-row dash-row-2">
            <article className="dash-card dash-card-wide">
              <header className="dash-card-head">
                <div>
                  <h3>Activité des missions</h3>
                  <p>Répartition par jour de planification — survolez une barre.</p>
                </div>
              </header>
              <MissionWeekChart missions={managerStats.missions} />
            </article>

            <article className="dash-card">
              <header className="dash-card-head">
                <div>
                  <h3>Avancement</h3>
                  <p>Survolez un segment du graphique.</p>
                </div>
              </header>
              <MissionStatusChart missions={managerStats.missions} />
            </article>
          </div>

          <div className="dash-row dash-row-3">
            <article className="dash-card">
              <header className="dash-card-head">
                <div>
                  <h3>Prochaines missions</h3>
                  <p>Planifiées et en cours</p>
                </div>
                <Link to="/missions" className="dash-link">
                  Tout voir
                </Link>
              </header>
              {nextMissions.length === 0 ? (
                <p className="dash-empty">Aucune mission à venir.</p>
              ) : (
                <ul className="dash-list">
                  {nextMissions.map((mission) => (
                    <li key={mission.id}>
                      <Link to={`/missions/${mission.id}`}>
                        <strong>{mission.reference}</strong>
                        <span>
                          {mission.departureLocation} → {mission.destination}
                        </span>
                      </Link>
                      <div className="dash-list-meta">
                        <MissionStatusBadge status={mission.status} />
                        <time>
                          {new Date(mission.scheduledAt).toLocaleString('fr-FR', {
                            day: '2-digit',
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </time>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </article>

            <article className="dash-card">
              <header className="dash-card-head">
                <div>
                  <h3>Chauffeurs en mission</h3>
                  <p>Équipe actuellement sur la route</p>
                </div>
                <Link to="/drivers" className="dash-link">
                  Équipe
                </Link>
              </header>
              {driversOnMission.length === 0 ? (
                <p className="dash-empty">Aucun chauffeur en mission pour le moment.</p>
              ) : (
                <ul className="dash-list">
                  {driversOnMission.map((driver) => (
                    <li key={driver.id}>
                      <Link to={`/drivers/${driver.id}`}>
                        <strong>
                          {driver.firstName} {driver.lastName}
                        </strong>
                        <span>{driver.phone}</span>
                      </Link>
                      <DriverStatusBadge status={driver.status} />
                    </li>
                  ))}
                </ul>
              )}
            </article>

            <article className="dash-card dash-card-alert">
              <header className="dash-card-head">
                <div>
                  <h3>Rappels</h3>
                  <p>Permis et documents véhicules</p>
                </div>
              </header>
              {managerStats.expiringLicenses.length === 0 && managerStats.expiringVehicles.length === 0 ? (
                <p className="dash-empty">Aucun rappel dans les 30 jours.</p>
              ) : (
                <ul className="dash-list">
                  {managerStats.expiringLicenses.map((driver) => (
                    <li key={driver.id}>
                      <Link to={`/drivers/${driver.id}`}>
                        <strong>
                          {driver.firstName} {driver.lastName}
                        </strong>
                        <span>
                          {driver.licenseExpired
                            ? 'Permis expiré'
                            : `Permis le ${new Date(driver.licenseExpiresAt).toLocaleDateString('fr-FR')}`}
                        </span>
                      </Link>
                    </li>
                  ))}
                  {managerStats.expiringVehicles.map((vehicle) => (
                    <li key={vehicle.id}>
                      <Link to={`/vehicles/${vehicle.id}`}>
                        <strong>{vehicle.registrationNumber}</strong>
                        <span>{vehicleAlertLabel(vehicle)}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </article>
          </div>
        </>
      ) : driverStats ? (
        <>
          <div className="kpi-grid kpi-grid-featured">
            <StatCard
              label="Mes missions"
              value={driverStats.missions.length}
              hint={`${driverStats.missionsPlanified} ${MISSION_STATUS_LABELS.PLANIFIED.toLowerCase()}`}
              to="/missions"
              featured
            />
            <StatCard label="En cours" value={driverStats.missionsInProgress} to="/missions" />
            <StatCard label="Planifiées" value={driverStats.missionsPlanified} to="/missions" />
          </div>

          <div className="dash-row dash-row-2">
            <article className="dash-card dash-card-wide">
              <header className="dash-card-head">
                <div>
                  <h3>Mes missions de la semaine</h3>
                  <p>Survolez les barres pour le détail.</p>
                </div>
              </header>
              <MissionWeekChart missions={driverStats.missions} />
            </article>
            <article className="dash-card">
              <header className="dash-card-head">
                <div>
                  <h3>Répartition</h3>
                </div>
              </header>
              <MissionStatusChart missions={driverStats.missions} />
            </article>
          </div>

          {nextMissions.length > 0 && (
            <article className="dash-card">
              <header className="dash-card-head">
                <h3>À venir</h3>
              </header>
              <ul className="dash-list">
                {nextMissions.map((mission) => (
                  <li key={mission.id}>
                    <Link to={`/missions/${mission.id}`}>
                      <strong>{mission.reference}</strong>
                      <span>
                        {mission.departureLocation} → {mission.destination}
                      </span>
                    </Link>
                    <MissionStatusBadge status={mission.status} />
                  </li>
                ))}
              </ul>
            </article>
          )}

          {driverStats.profile && (
            <article className={`dash-card${licenseWarning ? ' kpi-warning' : ''}`}>
              <h3>Mon statut</h3>
              <dl className="meta-list">
                <div>
                  <dt>Disponibilité</dt>
                  <dd>
                    <DriverStatusBadge status={driverStats.profile.status} />
                  </dd>
                </div>
                <div>
                  <dt>Permis</dt>
                  <dd>
                    {driverStats.profile.licenseExpired
                      ? 'Expiré — à renouveler'
                      : driverStats.profile.licenseExpiringSoon
                        ? `Expire dans ${driverStats.profile.daysUntilLicenseExpiry} jours`
                        : `Valide (${driverStats.profile.daysUntilLicenseExpiry} jours)`}
                  </dd>
                </div>
              </dl>
              <Link to="/profile" className="btn btn-secondary btn-inline">
                Voir mon profil
              </Link>
            </article>
          )}
        </>
      ) : null}
    </section>
  )
}

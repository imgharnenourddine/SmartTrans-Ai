import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { MissionStatusBadge } from '../../components/missions/MissionStatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { missionService } from '../../services/missionService'
import type { Mission, MissionStatus } from '../../types/mission'
import { MISSION_STATUS_LABELS, driverDisplayName, matchesMissionSearch } from '../../types/mission'
import { getApiErrorMessage } from '../../utils/apiError'

const STATUS_OPTIONS: Array<{ value: '' | MissionStatus; label: string }> = [
  { value: '', label: 'Tous les statuts' },
  ...((Object.keys(MISSION_STATUS_LABELS) as MissionStatus[]).map((status) => ({
    value: status,
    label: MISSION_STATUS_LABELS[status],
  }))),
]

export function MissionsListPage() {
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'TRANSPORT_MANAGER')
  const [missions, setMissions] = useState<Mission[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<'' | MissionStatus>('')

  const loadMissions = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await missionService.list()
      setMissions(data)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les missions'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadMissions()
  }, [loadMissions])

  const visibleMissions = useMemo(
    () =>
      missions.filter((mission) => {
        if (status && mission.status !== status) {
          return false
        }
        return matchesMissionSearch(mission, search)
      }),
    [missions, search, status],
  )

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>{canManage ? 'Missions' : 'Mes missions'}</h2>
          <p>
            {canManage
              ? 'Planification, assignation et suivi des transports.'
              : 'Missions qui vous sont assignées.'}
          </p>
        </div>
        {canManage && (
          <Link to="/missions/new" className="btn btn-primary btn-inline">
            Créer une mission
          </Link>
        )}
      </header>

      <div className="filters-panel">
        <label className="field">
          Recherche
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Référence, client, trajet…"
          />
        </label>

        <label className="field">
          Statut
          <select value={status} onChange={(event) => setStatus(event.target.value as '' | MissionStatus)}>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.label} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <button type="button" className="btn btn-secondary btn-inline" onClick={() => void loadMissions()}>
          Actualiser
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des missions…</p>
      ) : visibleMissions.length === 0 ? (
        <div className="empty-state">Aucune mission ne correspond aux critères.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Référence</th>
                <th>Client</th>
                <th>Trajet</th>
                <th>Planifiée</th>
                {canManage && <th>Chauffeur</th>}
                <th>Véhicule</th>
                <th>Statut</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visibleMissions.map((mission) => (
                <tr key={mission.id}>
                  <td>
                    <strong>{mission.reference}</strong>
                  </td>
                  <td>{mission.clientName ?? '—'}</td>
                  <td>
                    {mission.departureLocation} → {mission.destination}
                  </td>
                  <td>{new Date(mission.scheduledAt).toLocaleString('fr-FR')}</td>
                  {canManage && <td>{driverDisplayName(mission)}</td>}
                  <td>{mission.vehicleRegistrationNumber ?? '—'}</td>
                  <td>
                    <MissionStatusBadge status={mission.status} />
                  </td>
                  <td className="table-actions">
                    <Link to={`/missions/${mission.id}`}>Voir</Link>
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

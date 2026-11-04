import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { activityService } from '../../services/activityService'
import type { ActivityAction, ActivityRecord } from '../../types/activity'
import { ACTIVITY_ACTION_LABELS, activitySubject, entityTypeLabel, matchesActivitySearch } from '../../types/activity'
import { getApiErrorMessage } from '../../utils/apiError'

const ACTION_OPTIONS: Array<{ value: '' | ActivityAction; label: string }> = [
  { value: '', label: 'Toutes les actions' },
  ...(Object.keys(ACTIVITY_ACTION_LABELS) as ActivityAction[]).map((action) => ({
    value: action,
    label: ACTIVITY_ACTION_LABELS[action],
  })),
]

export function ActivitiesListPage() {
  const [activities, setActivities] = useState<ActivityRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [action, setAction] = useState<'' | ActivityAction>('')

  const loadActivities = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await activityService.list()
      setActivities(data)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les activités'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadActivities()
  }, [loadActivities])

  const visibleActivities = useMemo(() => {
    return activities
      .filter((activity) => {
        if (action && activity.action !== action) {
          return false
        }
        return matchesActivitySearch(activity, search)
      })
      .slice()
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [activities, search, action])

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>Activités</h2>
          <p>Journal d’audit généré automatiquement (missions, chauffeurs, véhicules, clients, utilisateurs, connexions).</p>
        </div>
      </header>

      <div className="filters-panel">
        <label className="field">
          Recherche
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Action, mission, chauffeur, immatriculation…"
          />
        </label>
        <label className="field">
          Action
          <select value={action} onChange={(event) => setAction(event.target.value as '' | ActivityAction)}>
            {ACTION_OPTIONS.map((option) => (
              <option key={option.label} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="btn btn-secondary btn-inline" onClick={() => void loadActivities()}>
          Actualiser
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des activités…</p>
      ) : visibleActivities.length === 0 ? (
        <div className="empty-state">Aucune activité ne correspond aux critères.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Action</th>
                <th>Entité</th>
                <th>Objet</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visibleActivities.map((activity) => (
                <tr key={activity.id}>
                  <td>{new Date(activity.createdAt).toLocaleString('fr-FR')}</td>
                  <td>{ACTIVITY_ACTION_LABELS[activity.action]}</td>
                  <td>{entityTypeLabel(activity.entityType)}</td>
                  <td>{activitySubject(activity)}</td>
                  <td className="table-actions">
                    <Link to={`/activities/${activity.id}`}>Voir</Link>
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

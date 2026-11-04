import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { activityService } from '../../services/activityService'
import { userService } from '../../services/userService'
import type { ActivityRecord } from '../../types/activity'
import {
  ACTIVITY_ACTION_LABELS,
  activitySubject,
  entityDetailPath,
  entityTypeLabel,
  formatActivityMetadata,
} from '../../types/activity'
import { getApiErrorMessage } from '../../utils/apiError'

export function ActivityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const [activity, setActivity] = useState<ActivityRecord | null>(null)
  const [actorLabel, setActorLabel] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadActivity() {
      setLoading(true)
      setError(null)
      try {
        const data = await activityService.getById(id!)
        setActivity(data)
        if (data.actorUserId && hasRole('ADMIN')) {
          try {
            const actor = await userService.getById(data.actorUserId)
            setActorLabel(`${actor.firstName} ${actor.lastName} (${actor.email})`)
          } catch {
            setActorLabel(null)
          }
        }
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger l’activité'))
      } finally {
        setLoading(false)
      }
    }

    void loadActivity()
  }, [id, hasRole])

  if (loading) {
    return <p className="page-loading">Chargement de l’activité…</p>
  }

  if (!activity) {
    return (
      <section className="vehicles-page">
        <p className="form-error">{error ?? 'Activité introuvable'}</p>
        <Link to="/activities">Retour au journal</Link>
      </section>
    )
  }

  const subject = activitySubject(activity)
  const entityPath = activity.entityId ? entityDetailPath(activity.entityType, activity.entityId) : null

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>{ACTIVITY_ACTION_LABELS[activity.action]}</h2>
          <p>
            <Link to="/activities">← Retour au journal</Link>
          </p>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <article className="info-block">
        <h3>Détail</h3>
        <dl className="meta-list">
          <div>
            <dt>Action</dt>
            <dd>{ACTIVITY_ACTION_LABELS[activity.action]}</dd>
          </div>
          <div>
            <dt>Type d’entité</dt>
            <dd>
              {entityTypeLabel(activity.entityType)} ({activity.entityType})
            </dd>
          </div>
          <div>
            <dt>Objet</dt>
            <dd>
              {entityPath ? <Link to={entityPath}>{subject}</Link> : subject}
            </dd>
          </div>
          <div>
            <dt>Auteur</dt>
            <dd>
              {activity.actorUserId ? (
                hasRole('ADMIN') ? (
                  <Link to={`/users/${activity.actorUserId}`}>{actorLabel ?? activity.actorUserId}</Link>
                ) : (
                  activity.actorUserId
                )
              ) : (
                '—'
              )}
            </dd>
          </div>
          <div>
            <dt>Date</dt>
            <dd>{new Date(activity.createdAt).toLocaleString('fr-FR')}</dd>
          </div>
        </dl>
      </article>

      <article className="info-block">
        <h3>Métadonnées</h3>
        <pre className="metadata-block">{formatActivityMetadata(activity.metadata)}</pre>
      </article>

      <button type="button" className="btn btn-ghost btn-inline back-link" onClick={() => navigate('/activities')}>
        Retour au journal
      </button>
    </section>
  )
}

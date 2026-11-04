import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { MissionStatusBadge } from '../../components/missions/MissionStatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { missionService } from '../../services/missionService'
import type { Mission, MissionStatus } from '../../types/mission'
import {
  MISSION_STATUS_ACTIONS,
  allowedStatusTransitions,
  canDeleteMission,
  driverDisplayName,
} from '../../types/mission'
import { getApiErrorMessage } from '../../utils/apiError'

export function MissionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'TRANSPORT_MANAGER')
  const canChangeStatus = hasRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')

  const [mission, setMission] = useState<Mission | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadMission() {
      setLoading(true)
      setError(null)
      try {
        const data = await missionService.getById(id!)
        setMission(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger la mission'))
      } finally {
        setLoading(false)
      }
    }

    void loadMission()
  }, [id])

  async function handleStatus(nextStatus: MissionStatus) {
    if (!mission) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      const updated = await missionService.updateStatus(mission.id, nextStatus)
      setMission(updated)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de changer le statut'))
    } finally {
      setActionLoading(false)
    }
  }

  async function handleDelete() {
    if (!mission || !window.confirm(`Supprimer la mission ${mission.reference} ?`)) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      await missionService.remove(mission.id)
      navigate('/missions')
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de supprimer la mission'))
      setActionLoading(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement de la mission…</p>
  }

  if (!mission) {
    return (
      <section className="vehicles-page">
        <p className="form-error">{error ?? 'Mission introuvable'}</p>
        <Link to="/missions">Retour à la liste</Link>
      </section>
    )
  }

  const nextStatuses = allowedStatusTransitions(mission.status).filter(
    (status) => canManage || status !== 'CANCELLED',
  )

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>{mission.reference}</h2>
          <p>
            <Link to="/missions">← Retour à la liste</Link>
          </p>
        </div>
        <div className="detail-actions">
          {canManage && (
            <Link to={`/missions/${mission.id}/edit`} className="btn btn-secondary btn-inline">
              Modifier
            </Link>
          )}
          {canChangeStatus &&
            nextStatuses.map((status) => (
              <button
                key={status}
                type="button"
                className={status === 'CANCELLED' ? 'btn btn-danger btn-inline' : 'btn btn-secondary btn-inline'}
                disabled={actionLoading}
                onClick={() => void handleStatus(status)}
              >
                {MISSION_STATUS_ACTIONS[status]}
              </button>
            ))}
          {canManage && canDeleteMission(mission.status) && (
            <button
              type="button"
              className="btn btn-danger btn-inline"
              disabled={actionLoading}
              onClick={() => void handleDelete()}
            >
              Supprimer
            </button>
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
                <MissionStatusBadge status={mission.status} />
              </dd>
            </div>
            <div>
              <dt>Client</dt>
              <dd>{mission.clientName ?? mission.clientId}</dd>
            </div>
            <div>
              <dt>Départ</dt>
              <dd>{mission.departureLocation}</dd>
            </div>
            <div>
              <dt>Destination</dt>
              <dd>{mission.destination}</dd>
            </div>
            <div>
              <dt>Marchandise</dt>
              <dd>{mission.cargoType ?? '—'}</dd>
            </div>
            <div>
              <dt>Poids</dt>
              <dd>{mission.weightKg !== null ? `${mission.weightKg.toLocaleString('fr-FR')} kg` : '—'}</dd>
            </div>
          </dl>
        </article>

        <article className="info-block">
          <h3>Assignation et dates</h3>
          <dl className="meta-list">
            <div>
              <dt>Chauffeur</dt>
              <dd>{driverDisplayName(mission)}</dd>
            </div>
            <div>
              <dt>Véhicule</dt>
              <dd>{mission.vehicleRegistrationNumber ?? '—'}</dd>
            </div>
            <div>
              <dt>Planifiée le</dt>
              <dd>{new Date(mission.scheduledAt).toLocaleString('fr-FR')}</dd>
            </div>
            <div>
              <dt>Démarrée le</dt>
              <dd>{mission.startedAt ? new Date(mission.startedAt).toLocaleString('fr-FR') : '—'}</dd>
            </div>
            <div>
              <dt>Terminée le</dt>
              <dd>{mission.completedAt ? new Date(mission.completedAt).toLocaleString('fr-FR') : '—'}</dd>
            </div>
          </dl>
        </article>
      </div>

      {mission.observations && (
        <article className="info-block">
          <h3>Observations</h3>
          <p>{mission.observations}</p>
        </article>
      )}

      <button type="button" className="btn btn-ghost btn-inline back-link" onClick={() => navigate('/missions')}>
        Retour à la liste
      </button>
    </section>
  )
}

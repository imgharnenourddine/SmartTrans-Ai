import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { MissionForm } from '../../components/missions/MissionForm'
import { missionService } from '../../services/missionService'
import type { Mission, UpdateMissionRequest } from '../../types/mission'
import { canReassignMission, toDatetimeLocalValue } from '../../types/mission'
import { getApiErrorMessage } from '../../utils/apiError'

export function MissionEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [mission, setMission] = useState<Mission | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
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

  async function handleSubmit(payload: UpdateMissionRequest) {
    if (!id) {
      return
    }

    setSaving(true)
    setError(null)
    try {
      const updated = await missionService.update(id, payload)
      navigate(`/missions/${updated.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour la mission'))
      setSaving(false)
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

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Modifier {mission.reference}</h2>
        <p>
          <Link to={`/missions/${mission.id}`}>← Retour au détail</Link>
        </p>
      </header>

      <article className="info-block">
        <MissionForm
          submitLabel="Enregistrer les modifications"
          loading={saving}
          error={error}
          lockAssignments={!canReassignMission(mission.status)}
          initialValues={{
            reference: mission.reference,
            clientId: mission.clientId,
            driverId: mission.driverId ?? '',
            vehicleId: mission.vehicleId ?? '',
            departureLocation: mission.departureLocation,
            destination: mission.destination,
            scheduledAt: toDatetimeLocalValue(mission.scheduledAt),
            weightKg: mission.weightKg !== null ? String(mission.weightKg) : '',
            cargoType: mission.cargoType ?? '',
            observations: mission.observations ?? '',
          }}
          onSubmit={handleSubmit}
        />
      </article>
    </section>
  )
}

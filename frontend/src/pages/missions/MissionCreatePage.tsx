import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MissionForm } from '../../components/missions/MissionForm'
import { missionService } from '../../services/missionService'
import type { CreateMissionRequest, UpdateMissionRequest } from '../../types/mission'
import { getApiErrorMessage } from '../../utils/apiError'

export function MissionCreatePage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(payload: CreateMissionRequest | UpdateMissionRequest) {
    setLoading(true)
    setError(null)
    try {
      const created = await missionService.create(payload as CreateMissionRequest)
      navigate(`/missions/${created.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer la mission'))
      setLoading(false)
    }
  }

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Nouvelle mission</h2>
        <p>
          <Link to="/missions">← Retour à la liste</Link>
        </p>
      </header>

      <article className="info-block">
        <MissionForm submitLabel="Créer la mission" loading={loading} error={error} onSubmit={handleSubmit} />
      </article>
    </section>
  )
}

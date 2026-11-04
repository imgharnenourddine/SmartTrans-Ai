import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { DriverForm } from '../../components/drivers/DriverForm'
import { driverService } from '../../services/driverService'
import type { CreateDriverRequest, UpdateDriverRequest } from '../../types/driver'
import { getApiErrorMessage } from '../../utils/apiError'

export function DriverCreatePage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(payload: CreateDriverRequest | UpdateDriverRequest) {
    setLoading(true)
    setError(null)
    try {
      const created = await driverService.create(payload as CreateDriverRequest)
      navigate(`/drivers/${created.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer le chauffeur'))
      setLoading(false)
    }
  }

  return (
    <section className="drivers-page">
      <header className="page-header">
        <h2>Nouveau chauffeur</h2>
        <p>
          <Link to="/drivers">← Retour à la liste</Link>
        </p>
      </header>

      <article className="info-block">
        <DriverForm submitLabel="Créer le chauffeur" loading={loading} error={error} onSubmit={handleSubmit} />
      </article>
    </section>
  )
}

import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ClientForm } from '../../components/clients/ClientForm'
import { clientService } from '../../services/clientService'
import type { CreateClientRequest, UpdateClientRequest } from '../../types/client'
import { getApiErrorMessage } from '../../utils/apiError'

export function ClientCreatePage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(payload: CreateClientRequest | UpdateClientRequest) {
    setLoading(true)
    setError(null)
    try {
      const created = await clientService.create(payload as CreateClientRequest)
      navigate(`/clients/${created.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer le client'))
      setLoading(false)
    }
  }

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Nouveau client</h2>
        <p>
          <Link to="/clients">← Retour à la liste</Link>
        </p>
      </header>

      <article className="info-block">
        <ClientForm submitLabel="Créer le client" loading={loading} error={error} onSubmit={handleSubmit} />
      </article>
    </section>
  )
}

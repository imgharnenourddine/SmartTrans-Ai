import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { clientService } from '../../services/clientService'
import type { Client } from '../../types/client'
import { getApiErrorMessage } from '../../utils/apiError'

export function ClientDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [client, setClient] = useState<Client | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadClient() {
      setLoading(true)
      setError(null)
      try {
        const data = await clientService.getById(id!)
        setClient(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger le client'))
      } finally {
        setLoading(false)
      }
    }

    void loadClient()
  }, [id])

  async function handleDelete() {
    if (!client || !window.confirm(`Supprimer le client ${client.companyName} ?`)) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      await clientService.remove(client.id)
      navigate('/clients')
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de supprimer le client'))
      setActionLoading(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement du client…</p>
  }

  if (!client) {
    return (
      <section className="vehicles-page">
        <p className="form-error">{error ?? 'Client introuvable'}</p>
        <Link to="/clients">Retour à la liste</Link>
      </section>
    )
  }

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>{client.companyName}</h2>
          <p>
            <Link to="/clients">← Retour à la liste</Link>
          </p>
        </div>
        <div className="detail-actions">
          <Link to={`/clients/${client.id}/edit`} className="btn btn-secondary btn-inline">
            Modifier
          </Link>
          <button
            type="button"
            className="btn btn-danger btn-inline"
            disabled={actionLoading}
            onClick={() => void handleDelete()}
          >
            Supprimer
          </button>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <article className="info-block">
        <h3>Coordonnées</h3>
        <dl className="meta-list">
          <div>
            <dt>Nom de l'entreprise</dt>
            <dd>{client.companyName}</dd>
          </div>
          <div>
            <dt>Téléphone</dt>
            <dd>{client.phone}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{client.email ?? '—'}</dd>
          </div>
          <div>
            <dt>Adresse</dt>
            <dd>{client.address}</dd>
          </div>
          <div>
            <dt>Créé le</dt>
            <dd>{new Date(client.createdAt).toLocaleString('fr-FR')}</dd>
          </div>
        </dl>
      </article>

      <button type="button" className="btn btn-ghost btn-inline back-link" onClick={() => navigate('/clients')}>
        Retour à la liste
      </button>
    </section>
  )
}

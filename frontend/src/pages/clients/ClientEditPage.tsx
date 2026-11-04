import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ClientForm } from '../../components/clients/ClientForm'
import { clientService } from '../../services/clientService'
import type { Client, UpdateClientRequest } from '../../types/client'
import { getApiErrorMessage } from '../../utils/apiError'

export function ClientEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [client, setClient] = useState<Client | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
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

  async function handleSubmit(payload: UpdateClientRequest) {
    if (!id) {
      return
    }

    setSaving(true)
    setError(null)
    try {
      const updated = await clientService.update(id, payload)
      navigate(`/clients/${updated.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour le client'))
      setSaving(false)
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
      <header className="page-header">
        <h2>Modifier {client.companyName}</h2>
        <p>
          <Link to={`/clients/${client.id}`}>← Retour au détail</Link>
        </p>
      </header>

      <article className="info-block">
        <ClientForm
          submitLabel="Enregistrer les modifications"
          loading={saving}
          error={error}
          initialValues={{
            companyName: client.companyName,
            address: client.address,
            phone: client.phone,
            email: client.email ?? '',
          }}
          onSubmit={handleSubmit}
        />
      </article>
    </section>
  )
}

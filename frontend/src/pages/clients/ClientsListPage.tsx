import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientService } from '../../services/clientService'
import type { Client } from '../../types/client'
import { matchesClientSearch } from '../../types/client'
import { getApiErrorMessage } from '../../utils/apiError'

export function ClientsListPage() {
  const [clients, setClients] = useState<Client[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const loadClients = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await clientService.list()
      setClients(data)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les clients'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadClients()
  }, [loadClients])

  const visibleClients = useMemo(
    () => clients.filter((client) => matchesClientSearch(client, search)),
    [clients, search],
  )

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>Clients</h2>
          <p>Entreprises destinataires des missions de transport.</p>
        </div>
        <Link to="/clients/new" className="btn btn-primary btn-inline">
          Ajouter un client
        </Link>
      </header>

      <div className="filters-panel">
        <label className="field">
          Recherche
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Nom de l'entreprise, téléphone, email…"
          />
        </label>

        <button type="button" className="btn btn-secondary btn-inline" onClick={() => void loadClients()}>
          Actualiser
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des clients…</p>
      ) : visibleClients.length === 0 ? (
        <div className="empty-state">Aucun client ne correspond aux critères.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nom de l'entreprise</th>
                <th>Téléphone</th>
                <th>Email</th>
                <th>Adresse</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visibleClients.map((client) => (
                <tr key={client.id}>
                  <td>
                    <strong>{client.companyName}</strong>
                  </td>
                  <td>{client.phone}</td>
                  <td>{client.email ?? '—'}</td>
                  <td>{client.address}</td>
                  <td className="table-actions">
                    <Link to={`/clients/${client.id}`}>Voir</Link>
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

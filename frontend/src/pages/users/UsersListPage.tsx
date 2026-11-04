import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { userService } from '../../services/userService'
import type { UserAccount } from '../../types/user'
import { matchesUserSearch, roleLabels } from '../../types/user'
import { getApiErrorMessage } from '../../utils/apiError'

export function UsersListPage() {
  const [users, setUsers] = useState<UserAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const loadUsers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await userService.list()
      setUsers(data)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les utilisateurs'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadUsers()
  }, [loadUsers])

  const visibleUsers = useMemo(
    () => users.filter((user) => matchesUserSearch(user, search)),
    [users, search],
  )

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>Utilisateurs</h2>
          <p>Comptes de connexion et attribution des rôles.</p>
        </div>
        <div className="detail-actions">
          <Link to="/roles" className="btn btn-secondary btn-inline">
            Rôles
          </Link>
          <Link to="/users/new" className="btn btn-primary btn-inline">
            Créer un utilisateur
          </Link>
        </div>
      </header>

      <div className="filters-panel">
        <label className="field">
          Recherche
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Nom, email, rôle…"
          />
        </label>
        <button type="button" className="btn btn-secondary btn-inline" onClick={() => void loadUsers()}>
          Actualiser
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des utilisateurs…</p>
      ) : visibleUsers.length === 0 ? (
        <div className="empty-state">Aucun utilisateur ne correspond aux critères.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Rôles</th>
                <th>Statut</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visibleUsers.map((user) => (
                <tr key={user.id}>
                  <td>
                    <strong>
                      {user.firstName} {user.lastName}
                    </strong>
                  </td>
                  <td>{user.email}</td>
                  <td>{roleLabels(user.roleCodes)}</td>
                  <td>
                    <span className={user.enabled ? 'status-badge status-available' : 'status-badge status-suspended'}>
                      {user.enabled ? 'Actif' : 'Désactivé'}
                    </span>
                  </td>
                  <td className="table-actions">
                    <Link to={`/users/${user.id}`}>Voir</Link>
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

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { roleService } from '../../services/roleService'
import type { Role } from '../../types/role'
import { ROLE_LABELS } from '../../types/role'
import { getApiErrorMessage } from '../../utils/apiError'

export function RolesListPage() {
  const [roles, setRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function loadRoles() {
      setLoading(true)
      setError(null)
      try {
        const data = await roleService.list()
        setRoles(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger les rôles'))
      } finally {
        setLoading(false)
      }
    }

    void loadRoles()
  }, [])

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Rôles</h2>
        <p>
          Catalogue des rôles utilisés à la création des comptes.{' '}
          <Link to="/users">← Utilisateurs</Link>
        </p>
      </header>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <p className="page-loading">Chargement des rôles…</p>
      ) : roles.length === 0 ? (
        <div className="empty-state">Aucun rôle n’est défini.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Libellé</th>
              </tr>
            </thead>
            <tbody>
              {roles.map((role) => (
                <tr key={role.id}>
                  <td>
                    <strong>{role.code}</strong>
                  </td>
                  <td>{role.label || ROLE_LABELS[role.code]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { userService } from '../../services/userService'
import type { UserAccount } from '../../types/user'
import { roleLabels } from '../../types/user'
import { getApiErrorMessage } from '../../utils/apiError'

export function UserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user: currentUser } = useAuth()
  const [user, setUser] = useState<UserAccount | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadUser() {
      setLoading(true)
      setError(null)
      try {
        const data = await userService.getById(id!)
        setUser(data)
      } catch (err) {
        setError(getApiErrorMessage(err, "Impossible de charger l'utilisateur"))
      } finally {
        setLoading(false)
      }
    }

    void loadUser()
  }, [id])

  async function handleDelete() {
    if (!user || !window.confirm(`Supprimer le compte ${user.email} ?`)) {
      return
    }

    setActionLoading(true)
    setError(null)
    try {
      await userService.remove(user.id)
      navigate('/users')
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de supprimer l'utilisateur"))
      setActionLoading(false)
    }
  }

  async function copyId() {
    if (!user) {
      return
    }
    await navigator.clipboard.writeText(user.id)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 2000)
  }

  if (loading) {
    return <p className="page-loading">Chargement de l'utilisateur…</p>
  }

  if (!user) {
    return (
      <section className="vehicles-page">
        <p className="form-error">{error ?? 'Utilisateur introuvable'}</p>
        <Link to="/users">Retour à la liste</Link>
      </section>
    )
  }

  const isCurrentUser = currentUser?.userId === user.id

  return (
    <section className="vehicles-page">
      <header className="page-header page-header-row">
        <div>
          <h2>
            {user.firstName} {user.lastName}
          </h2>
          <p>
            <Link to="/users">← Retour à la liste</Link>
          </p>
        </div>
        <div className="detail-actions">
          <Link to={`/users/${user.id}/edit`} className="btn btn-secondary btn-inline">
            Modifier
          </Link>
          <button
            type="button"
            className="btn btn-danger btn-inline"
            disabled={actionLoading || isCurrentUser}
            title={isCurrentUser ? 'Vous ne pouvez pas supprimer votre propre compte' : undefined}
            onClick={() => void handleDelete()}
          >
            Supprimer
          </button>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <article className="info-block">
        <h3>Compte</h3>
        <dl className="meta-list">
          <div>
            <dt>Email</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Téléphone</dt>
            <dd>{user.phone ?? '—'}</dd>
          </div>
          <div>
            <dt>Rôles</dt>
            <dd>{roleLabels(user.roleCodes)}</dd>
          </div>
          <div>
            <dt>Statut</dt>
            <dd>{user.enabled ? 'Actif' : 'Désactivé'}</dd>
          </div>
          <div>
            <dt>Dernière connexion</dt>
            <dd>{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('fr-FR') : '—'}</dd>
          </div>
          <div>
            <dt>ID (lien chauffeur)</dt>
            <dd>
              <code>{user.id}</code>{' '}
              <button type="button" className="btn btn-secondary btn-inline" onClick={() => void copyId()}>
                {copied ? 'Copié' : 'Copier'}
              </button>
            </dd>
          </div>
        </dl>
      </article>

      <button type="button" className="btn btn-ghost btn-inline back-link" onClick={() => navigate('/users')}>
        Retour à la liste
      </button>
    </section>
  )
}

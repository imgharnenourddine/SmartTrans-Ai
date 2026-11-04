import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { UserForm } from '../../components/users/UserForm'
import { userService } from '../../services/userService'
import type { UpdateUserRequest, UserAccount } from '../../types/user'
import { getApiErrorMessage } from '../../utils/apiError'

export function UserEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [user, setUser] = useState<UserAccount | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

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

  async function handleSubmit(payload: UpdateUserRequest) {
    if (!id) {
      return
    }

    setSaving(true)
    setError(null)
    try {
      const updated = await userService.update(id, payload)
      navigate(`/users/${updated.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de mettre à jour l'utilisateur"))
      setSaving(false)
    }
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

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>
          Modifier {user.firstName} {user.lastName}
        </h2>
        <p>
          <Link to={`/users/${user.id}`}>← Retour au détail</Link>
        </p>
      </header>

      <article className="info-block">
        <UserForm
          submitLabel="Enregistrer les modifications"
          loading={saving}
          error={error}
          showEnabled
          initialValues={{
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            phone: user.phone ?? '',
            enabled: user.enabled,
            roleCodes: user.roleCodes,
          }}
          onSubmit={handleSubmit}
        />
      </article>
    </section>
  )
}

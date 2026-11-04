import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserForm, type UserFormExtras } from '../../components/users/UserForm'
import { driverService } from '../../services/driverService'
import { userService } from '../../services/userService'
import type { CreateUserRequest, UpdateUserRequest } from '../../types/user'
import { getApiErrorMessage } from '../../utils/apiError'

export function UserCreatePage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(payload: CreateUserRequest | UpdateUserRequest, extras?: UserFormExtras) {
    setLoading(true)
    setError(null)
    try {
      const created = await userService.create(payload as CreateUserRequest)
      if (extras?.driverId) {
        await driverService.update(extras.driverId, { userId: created.id })
      }
      navigate(`/users/${created.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer l'utilisateur"))
      setLoading(false)
    }
  }

  return (
    <section className="vehicles-page">
      <header className="page-header">
        <h2>Nouvel utilisateur</h2>
        <p>
          <Link to="/users">← Retour à la liste</Link>
        </p>
      </header>

      <article className="info-block">
        <UserForm
          submitLabel="Créer l'utilisateur"
          loading={loading}
          error={error}
          requirePassword
          allowDriverLink
          onSubmit={handleSubmit}
        />
      </article>
    </section>
  )
}

import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../utils/apiError'

export function LoginPage() {
  const { login, isLoading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard'

  const [email, setEmail] = useState('admin@smarttrans.ai')
  const [password, setPassword] = useState('Password1!')
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    try {
      await login({ email: email.trim(), password })
      navigate(from, { replace: true })
    } catch (err) {
      setError(getApiErrorMessage(err, 'Identifiants invalides'))
    }
  }

  return (
    <div className="login-screen">
      <div className="login-backdrop" aria-hidden="true" />
      <section className="login-panel">
        <p className="login-brand">SmartTrans AI</p>
        <h1 className="login-title">Connexion</h1>
        <p className="login-subtitle">Accédez à la plateforme de gestion transport.</p>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              name="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={isLoading}
            />
          </label>

          <label className="field">
            <span>Mot de passe</span>
            <input
              type="password"
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              disabled={isLoading}
            />
          </label>

          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>
      </section>
    </div>
  )
}

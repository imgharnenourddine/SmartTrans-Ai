import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { DriverForm } from '../../components/drivers/DriverForm'
import { driverService } from '../../services/driverService'
import type { Driver, UpdateDriverRequest } from '../../types/driver'
import { getApiErrorMessage } from '../../utils/apiError'

export function DriverEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [driver, setDriver] = useState<Driver | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      return
    }

    async function loadDriver() {
      setLoading(true)
      setError(null)
      try {
        const data = await driverService.getById(id!)
        setDriver(data)
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de charger le chauffeur'))
      } finally {
        setLoading(false)
      }
    }

    void loadDriver()
  }, [id])

  async function handleSubmit(payload: UpdateDriverRequest) {
    if (!id) {
      return
    }

    setSaving(true)
    setError(null)
    try {
      const updated = await driverService.update(id, payload)
      navigate(`/drivers/${updated.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour le chauffeur'))
      setSaving(false)
    }
  }

  if (loading) {
    return <p className="page-loading">Chargement du chauffeur…</p>
  }

  if (!driver) {
    return (
      <section className="drivers-page">
        <p className="form-error">{error ?? 'Chauffeur introuvable'}</p>
        <Link to="/drivers">Retour à la liste</Link>
      </section>
    )
  }

  return (
    <section className="drivers-page">
      <header className="page-header">
        <h2>
          Modifier {driver.firstName} {driver.lastName}
        </h2>
        <p>
          <Link to={`/drivers/${driver.id}`}>← Retour au détail</Link>
        </p>
      </header>

      <article className="info-block">
        <DriverForm
          submitLabel="Enregistrer les modifications"
          loading={saving}
          error={error}
          initialValues={{
            firstName: driver.firstName,
            lastName: driver.lastName,
            phone: driver.phone,
            licenseNumber: driver.licenseNumber,
            licenseExpiresAt: driver.licenseExpiresAt,
            experienceYears: driver.experienceYears?.toString() ?? '',
            status: driver.status,
          }}
          onSubmit={handleSubmit}
        />
      </article>
    </section>
  )
}

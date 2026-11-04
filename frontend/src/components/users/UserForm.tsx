import { useEffect, useState, type FormEvent } from 'react'
import { driverService } from '../../services/driverService'
import { roleService } from '../../services/roleService'
import type { RoleCode } from '../../types/auth'
import type { Driver } from '../../types/driver'
import type { Role } from '../../types/role'
import { ROLE_LABELS } from '../../types/role'
import type { CreateUserRequest, UpdateUserRequest } from '../../types/user'
import { getApiErrorMessage } from '../../utils/apiError'

export type UserFormValues = {
  email: string
  password: string
  firstName: string
  lastName: string
  phone: string
  enabled: boolean
  roleCodes: RoleCode[]
  driverId: string
}

export type UserFormExtras = {
  driverId?: string
}

interface UserFormProps {
  initialValues?: Partial<UserFormValues>
  submitLabel: string
  loading?: boolean
  error?: string | null
  requirePassword?: boolean
  showEnabled?: boolean
  allowDriverLink?: boolean
  onSubmit: (payload: CreateUserRequest | UpdateUserRequest, extras?: UserFormExtras) => Promise<void>
}

const DEFAULT_VALUES: UserFormValues = {
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  phone: '',
  enabled: true,
  roleCodes: [],
  driverId: '',
}

export function UserForm({
  initialValues,
  submitLabel,
  loading = false,
  error = null,
  requirePassword = false,
  showEnabled = false,
  allowDriverLink = false,
  onSubmit,
}: UserFormProps) {
  const [values, setValues] = useState<UserFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
    roleCodes: initialValues?.roleCodes ?? DEFAULT_VALUES.roleCodes,
  })
  const [roles, setRoles] = useState<Role[]>([])
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [optionsError, setOptionsError] = useState<string | null>(null)

  const isDriverRole = values.roleCodes.includes('DRIVER')

  useEffect(() => {
    async function loadOptions() {
      try {
        const [roleData, driverData] = await Promise.all([
          roleService.list(),
          allowDriverLink ? driverService.list() : Promise.resolve([]),
        ])
        setRoles(roleData)
        setDrivers(driverData)
      } catch (err) {
        setOptionsError(getApiErrorMessage(err, 'Impossible de charger les listes'))
      }
    }

    void loadOptions()
  }, [allowDriverLink])

  function toggleRole(code: RoleCode) {
    setValues((current) => {
      const roleCodes = current.roleCodes.includes(code)
        ? current.roleCodes.filter((item) => item !== code)
        : [...current.roleCodes, code]
      return {
        ...current,
        roleCodes,
        driverId: roleCodes.includes('DRIVER') ? current.driverId : '',
      }
    })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (values.roleCodes.length === 0) {
      return
    }

    const payload: CreateUserRequest | UpdateUserRequest = {
      email: values.email.trim(),
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      phone: values.phone.trim() || null,
      roleCodes: values.roleCodes,
    }

    if (requirePassword) {
      ;(payload as CreateUserRequest).password = values.password
    }

    if (showEnabled) {
      ;(payload as UpdateUserRequest).enabled = values.enabled
    }

    await onSubmit(payload, {
      driverId: allowDriverLink && isDriverRole ? values.driverId || undefined : undefined,
    })
  }

  const roleOptions: Role[] =
    roles.length > 0
      ? roles
      : (Object.keys(ROLE_LABELS) as RoleCode[]).map((code) => ({
          id: code,
          code,
          label: ROLE_LABELS[code],
          createdAt: '',
        }))

  const linkableDrivers = drivers.filter(
    (driver) => !driver.userId || driver.id === values.driverId,
  )

  return (
    <form className="user-form" onSubmit={handleSubmit}>
      {(error || optionsError) && <p className="form-error">{error ?? optionsError}</p>}

      <div className="form-grid">
        <label className="field">
          Prénom
          <input
            required
            maxLength={100}
            value={values.firstName}
            onChange={(event) => setValues((current) => ({ ...current, firstName: event.target.value }))}
          />
        </label>

        <label className="field">
          Nom
          <input
            required
            maxLength={100}
            value={values.lastName}
            onChange={(event) => setValues((current) => ({ ...current, lastName: event.target.value }))}
          />
        </label>

        <label className="field">
          Email
          <input
            required
            type="email"
            maxLength={255}
            value={values.email}
            onChange={(event) => setValues((current) => ({ ...current, email: event.target.value }))}
          />
        </label>

        <label className="field">
          Téléphone
          <input
            maxLength={30}
            value={values.phone}
            onChange={(event) => setValues((current) => ({ ...current, phone: event.target.value }))}
          />
        </label>

        {requirePassword && (
          <label className="field">
            Mot de passe
            <input
              required
              type="password"
              minLength={8}
              maxLength={100}
              value={values.password}
              onChange={(event) => setValues((current) => ({ ...current, password: event.target.value }))}
              placeholder="8 caractères minimum"
            />
          </label>
        )}

        {showEnabled && (
          <label className="field checkbox-field">
            <span>Compte actif</span>
            <input
              type="checkbox"
              checked={values.enabled}
              onChange={(event) => setValues((current) => ({ ...current, enabled: event.target.checked }))}
            />
          </label>
        )}
      </div>

      <fieldset className="field role-fieldset">
        <legend>Rôles (au moins un)</legend>
        {roleOptions.map((role) => (
          <label key={role.code} className="checkbox-field">
            <span>
              {role.label} <span className="kpi-hint">({role.code})</span>
            </span>
            <input
              type="checkbox"
              checked={values.roleCodes.includes(role.code)}
              onChange={() => toggleRole(role.code)}
            />
          </label>
        ))}
      </fieldset>

      {allowDriverLink && isDriverRole && (
        <label className="field">
          Chauffeur existant à lier
          <select
            required
            value={values.driverId}
            onChange={(event) => setValues((current) => ({ ...current, driverId: event.target.value }))}
          >
            <option value="">Sélectionner un chauffeur</option>
            {linkableDrivers.map((driver) => (
              <option key={driver.id} value={driver.id}>
                {driver.firstName} {driver.lastName} — {driver.licenseNumber}
              </option>
            ))}
          </select>
          {linkableDrivers.length === 0 && (
            <span className="kpi-hint">Aucun chauffeur sans compte. Créez d’abord la fiche chauffeur.</span>
          )}
        </label>
      )}

      <button
        type="submit"
        className="btn btn-primary btn-inline"
        disabled={loading || values.roleCodes.length === 0}
      >
        {loading ? 'Enregistrement…' : submitLabel}
      </button>
    </form>
  )
}

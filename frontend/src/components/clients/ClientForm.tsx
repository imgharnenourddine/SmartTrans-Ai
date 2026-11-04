import { useState, type FormEvent } from 'react'
import type { CreateClientRequest, UpdateClientRequest } from '../../types/client'

export type ClientFormValues = {
  companyName: string
  address: string
  phone: string
  email: string
}

interface ClientFormProps {
  initialValues?: Partial<ClientFormValues>
  submitLabel: string
  loading?: boolean
  error?: string | null
  onSubmit: (payload: CreateClientRequest | UpdateClientRequest) => Promise<void>
}

const DEFAULT_VALUES: ClientFormValues = {
  companyName: '',
  address: '',
  phone: '',
  email: '',
}

export function ClientForm({
  initialValues,
  submitLabel,
  loading = false,
  error = null,
  onSubmit,
}: ClientFormProps) {
  const [values, setValues] = useState<ClientFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
  })

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const payload: CreateClientRequest | UpdateClientRequest = {
      companyName: values.companyName.trim(),
      address: values.address.trim(),
      phone: values.phone.trim(),
      email: values.email.trim() || null,
    }

    await onSubmit(payload)
  }

  return (
    <form className="client-form" onSubmit={handleSubmit}>
      {error && <p className="form-error">{error}</p>}

      <div className="form-grid">
        <label className="field">
          Nom de l'entreprise
          <input
            required
            maxLength={255}
            value={values.companyName}
            onChange={(event) => setValues((current) => ({ ...current, companyName: event.target.value }))}
          />
        </label>

        <label className="field">
          Téléphone
          <input
            required
            maxLength={30}
            value={values.phone}
            onChange={(event) => setValues((current) => ({ ...current, phone: event.target.value }))}
          />
        </label>

        <label className="field">
          Email
          <input
            type="email"
            maxLength={255}
            value={values.email}
            onChange={(event) => setValues((current) => ({ ...current, email: event.target.value }))}
          />
        </label>
      </div>

      <label className="field">
        Adresse
        <textarea
          required
          rows={3}
          value={values.address}
          onChange={(event) => setValues((current) => ({ ...current, address: event.target.value }))}
        />
      </label>

      <button type="submit" className="btn btn-primary btn-inline" disabled={loading}>
        {loading ? 'Enregistrement…' : submitLabel}
      </button>
    </form>
  )
}

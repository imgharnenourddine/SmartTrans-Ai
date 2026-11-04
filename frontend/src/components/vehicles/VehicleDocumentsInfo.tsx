import type { Vehicle } from '../../types/vehicle'
import { daysUntil, documentState } from '../../types/vehicle'

interface VehicleDocumentsInfoProps {
  vehicle: Vehicle
}

const STATE_LABEL: Record<string, string> = {
  UNKNOWN: 'Non renseignée',
  EXPIRED: 'Expirée — le véhicule ne peut pas être disponible.',
  EXPIRING_SOON: 'Échéance proche — renouvellement recommandé.',
  VALID: 'Valide.',
}

function DocumentRow({ label, expiresAt }: { label: string; expiresAt: string | null }) {
  const state = documentState(expiresAt)
  const days = daysUntil(expiresAt)

  const alertClass =
    state === 'EXPIRED'
      ? 'license-alert license-expired'
      : state === 'EXPIRING_SOON'
        ? 'license-alert license-expiring'
        : 'license-alert license-valid'

  return (
    <div className="license-info">
      <dl className="meta-list">
        <div>
          <dt>{label}</dt>
          <dd>
            {expiresAt ? new Date(expiresAt).toLocaleDateString('fr-FR') : '—'}
            {state === 'EXPIRED' && <span className="inline-tag tag-danger">Expirée</span>}
            {state === 'EXPIRING_SOON' && <span className="inline-tag tag-warning">Bientôt</span>}
          </dd>
        </div>
        <div>
          <dt>Jours restants</dt>
          <dd>{days === null ? '—' : days}</dd>
        </div>
      </dl>
      <p className={alertClass}>{STATE_LABEL[state]}</p>
    </div>
  )
}

export function VehicleDocumentsInfo({ vehicle }: VehicleDocumentsInfoProps) {
  return (
    <div className="documents-info">
      <DocumentRow label="Expiration de l'assurance" expiresAt={vehicle.insuranceExpiresAt} />
      <DocumentRow label="Expiration de la visite technique" expiresAt={vehicle.technicalVisitExpiresAt} />
    </div>
  )
}

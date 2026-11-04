import type { Driver } from '../../types/driver'

interface DriverLicenseInfoProps {
  driver: Driver
}

export function DriverLicenseInfo({ driver }: DriverLicenseInfoProps) {
  const expiryClass = driver.licenseExpired
    ? 'license-alert license-expired'
    : driver.licenseExpiringSoon
      ? 'license-alert license-expiring'
      : 'license-alert license-valid'

  return (
    <div className="license-info">
      <dl className="meta-list">
        <div>
          <dt>Numéro de permis</dt>
          <dd>{driver.licenseNumber}</dd>
        </div>
        <div>
          <dt>Expiration</dt>
          <dd>{new Date(driver.licenseExpiresAt).toLocaleDateString('fr-FR')}</dd>
        </div>
        <div>
          <dt>Jours restants</dt>
          <dd>{driver.daysUntilLicenseExpiry}</dd>
        </div>
      </dl>
      <p className={expiryClass}>
        {driver.licenseExpired
          ? 'Permis expiré — le chauffeur ne peut pas être disponible.'
          : driver.licenseExpiringSoon
            ? 'Permis expirant bientôt — renouvellement recommandé.'
            : 'Permis valide.'}
      </p>
    </div>
  )
}

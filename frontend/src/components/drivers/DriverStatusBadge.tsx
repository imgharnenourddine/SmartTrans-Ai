import type { DriverStatus } from '../../types/driver'
import { DRIVER_STATUS_LABELS } from '../../types/driver'

interface DriverStatusBadgeProps {
  status: DriverStatus
}

const STATUS_CLASS: Record<DriverStatus, string> = {
  AVAILABLE: 'status-badge status-available',
  ON_MISSION: 'status-badge status-mission',
  UNAVAILABLE: 'status-badge status-unavailable',
  SUSPENDED: 'status-badge status-suspended',
}

export function DriverStatusBadge({ status }: DriverStatusBadgeProps) {
  return <span className={STATUS_CLASS[status]}>{DRIVER_STATUS_LABELS[status]}</span>
}

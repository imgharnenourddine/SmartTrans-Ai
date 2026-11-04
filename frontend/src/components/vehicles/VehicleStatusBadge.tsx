import type { VehicleStatus } from '../../types/vehicle'
import { VEHICLE_STATUS_LABELS } from '../../types/vehicle'

interface VehicleStatusBadgeProps {
  status: VehicleStatus
}

const STATUS_CLASS: Record<VehicleStatus, string> = {
  AVAILABLE: 'status-badge status-available',
  IN_USE: 'status-badge status-mission',
  MAINTENANCE: 'status-badge status-unavailable',
  OUT_OF_SERVICE: 'status-badge status-suspended',
}

export function VehicleStatusBadge({ status }: VehicleStatusBadgeProps) {
  return <span className={STATUS_CLASS[status]}>{VEHICLE_STATUS_LABELS[status]}</span>
}

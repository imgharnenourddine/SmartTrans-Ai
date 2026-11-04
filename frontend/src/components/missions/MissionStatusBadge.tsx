import type { MissionStatus } from '../../types/mission'
import { MISSION_STATUS_LABELS } from '../../types/mission'

interface MissionStatusBadgeProps {
  status: MissionStatus
}

const STATUS_CLASS: Record<MissionStatus, string> = {
  PLANIFIED: 'status-badge status-available',
  IN_PROGRESS: 'status-badge status-mission',
  COMPLETED: 'status-badge status-available',
  CANCELLED: 'status-badge status-suspended',
}

export function MissionStatusBadge({ status }: MissionStatusBadgeProps) {
  return <span className={STATUS_CLASS[status]}>{MISSION_STATUS_LABELS[status]}</span>
}

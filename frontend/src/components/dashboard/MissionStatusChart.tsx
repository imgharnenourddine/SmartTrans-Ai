import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { MISSION_STATUS_LABELS, type Mission, type MissionStatus } from '../../types/mission'

const COLORS: Record<MissionStatus, string> = {
  PLANIFIED: '#0f8a6a',
  IN_PROGRESS: '#7bc9a8',
  COMPLETED: '#0b3d32',
  CANCELLED: '#c5d2d6',
}

interface Slice {
  key: MissionStatus
  name: string
  value: number
}

interface MissionStatusChartProps {
  missions: Mission[]
}

export function MissionStatusChart({ missions }: MissionStatusChartProps) {
  const navigate = useNavigate()

  const data = useMemo<Slice[]>(() => {
    const counts: Record<MissionStatus, number> = {
      PLANIFIED: 0,
      IN_PROGRESS: 0,
      COMPLETED: 0,
      CANCELLED: 0,
    }
    for (const mission of missions) {
      counts[mission.status] += 1
    }
    return (Object.keys(counts) as MissionStatus[])
      .map((key) => ({ key, name: MISSION_STATUS_LABELS[key], value: counts[key] }))
      .filter((slice) => slice.value > 0)
  }, [missions])

  const completed = missions.filter((mission) => mission.status === 'COMPLETED').length
  const percent = missions.length === 0 ? 0 : Math.round((completed / missions.length) * 100)

  if (missions.length === 0) {
    return <p className="dash-empty">Aucune mission à afficher.</p>
  }

  return (
    <div className="dash-donut">
      <div className="dash-donut-chart">
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="85%"
              startAngle={180}
              endAngle={0}
              innerRadius={78}
              outerRadius={110}
              paddingAngle={3}
              onClick={() => navigate('/missions')}
              style={{ cursor: 'pointer' }}
            >
              {data.map((slice) => (
                <Cell key={slice.key} fill={COLORS[slice.key]} stroke="none" />
              ))}
            </Pie>
            <Tooltip
              cursor={false}
              content={({ active, payload }) => {
                if (!active || !payload?.[0]) {
                  return null
                }
                const item = payload[0]
                return (
                  <div className="dash-tooltip">
                    <strong>{item.name}</strong>
                    <span>
                      {item.value} mission{(item.value as number) > 1 ? 's' : ''}
                    </span>
                  </div>
                )
              }}
            />
          </PieChart>
        </ResponsiveContainer>
        <p className="dash-donut-center">
          <strong>{percent}%</strong>
          <span>terminées</span>
        </p>
      </div>
      <ul className="dash-legend">
        {(Object.keys(COLORS) as MissionStatus[]).map((status) => (
          <li key={status}>
            <span className="dash-swatch" style={{ background: COLORS[status] }} />
            {MISSION_STATUS_LABELS[status]}
          </li>
        ))}
      </ul>
    </div>
  )
}

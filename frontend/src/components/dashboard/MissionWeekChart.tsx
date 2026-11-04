import { useMemo } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { Mission } from '../../types/mission'

const DAYS = [
  { key: 1, label: 'L' },
  { key: 2, label: 'M' },
  { key: 3, label: 'M' },
  { key: 4, label: 'J' },
  { key: 5, label: 'V' },
  { key: 6, label: 'S' },
  { key: 0, label: 'D' },
]

interface MissionWeekChartProps {
  missions: Mission[]
}

export function MissionWeekChart({ missions }: MissionWeekChartProps) {
  const data = useMemo(() => {
    const buckets = DAYS.map((day) => ({
      label: day.label,
      weekday: day.key,
      Planifiées: 0,
      'En cours': 0,
      Terminées: 0,
    }))
    const indexByWeekday = new Map(DAYS.map((day, index) => [day.key, index]))
    for (const mission of missions) {
      const weekday = new Date(mission.scheduledAt).getDay()
      const index = indexByWeekday.get(weekday)
      if (index === undefined) {
        continue
      }
      if (mission.status === 'PLANIFIED') {
        buckets[index].Planifiées += 1
      } else if (mission.status === 'IN_PROGRESS') {
        buckets[index]['En cours'] += 1
      } else if (mission.status === 'COMPLETED') {
        buckets[index].Terminées += 1
      }
    }
    return buckets
  }, [missions])

  const hasValues = data.some((row) => row.Planifiées + row['En cours'] + row.Terminées > 0)

  if (!hasValues) {
    return <p className="dash-empty">Pas encore de missions planifiées sur la semaine.</p>
  }

  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={data} barGap={4} barCategoryGap="28%">
        <CartesianGrid vertical={false} stroke="#e6eeed" />
        <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fill: '#5b6f79', fontSize: 13 }} />
        <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fill: '#5b6f79', fontSize: 12 }} width={28} />
        <Tooltip
          cursor={{ fill: 'rgba(15, 138, 106, 0.08)', radius: 8 }}
          content={({ active, payload, label }) => {
            if (!active || !payload?.length) {
              return null
            }
            return (
              <div className="dash-tooltip">
                <strong>Jour {label}</strong>
                {payload.map((item) => (
                  <span key={String(item.name)}>
                    <i style={{ background: String(item.color) }} />
                    {item.name} : {item.value}
                  </span>
                ))}
              </div>
            )
          }}
        />
        <Bar dataKey="Planifiées" fill="#0f8a6a" radius={[8, 8, 0, 8]} maxBarSize={18} />
        <Bar dataKey="En cours" fill="#7bc9a8" radius={[8, 8, 0, 8]} maxBarSize={18} />
        <Bar dataKey="Terminées" fill="#0b3d32" radius={[8, 8, 0, 8]} maxBarSize={18} />
      </BarChart>
    </ResponsiveContainer>
  )
}

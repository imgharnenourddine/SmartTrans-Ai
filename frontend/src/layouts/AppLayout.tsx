import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

function Icon({ name }: { name: string }) {
  const common = {
    className: 'nav-icon',
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
  }

  switch (name) {
    case 'dashboard':
      return (
        <svg {...common}>
          <rect x="3" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="3" width="7" height="7" rx="1.5" />
          <rect x="3" y="14" width="7" height="7" rx="1.5" />
          <rect x="14" y="14" width="7" height="7" rx="1.5" />
        </svg>
      )
    case 'drivers':
      return (
        <svg {...common}>
          <circle cx="9" y="8" r="3" />
          <path d="M4 19a5 5 0 0 1 10 0" />
          <circle cx="17" y="9" r="2.2" />
          <path d="M16.2 19a4.2 4.2 0 0 1 5.3-3.6" />
        </svg>
      )
    case 'vehicles':
      return (
        <svg {...common}>
          <path d="M3 13h18v5H3z" />
          <path d="M5 13 7 7h10l2 6" />
          <circle cx="7.5" cy="18" r="1.5" />
          <circle cx="16.5" cy="18" r="1.5" />
        </svg>
      )
    case 'clients':
      return (
        <svg {...common}>
          <path d="M3 20V6a2 2 0 0 1 2-2h8v16H5a2 2 0 0 1-2-2Z" />
          <path d="M13 8h6a2 2 0 0 1 2 2v10h-8" />
        </svg>
      )
    case 'missions':
      return (
        <svg {...common}>
          <path d="M4 6h12l4 6-4 6H4V6Z" />
          <path d="M9 12h6" />
        </svg>
      )
    case 'assistant':
      return (
        <svg {...common}>
          <path d="M12 4v2" />
          <path d="M12 18v2" />
          <path d="M4 12h2" />
          <path d="M18 12h2" />
          <circle cx="12" cy="12" r="5" />
        </svg>
      )
    case 'activities':
      return (
        <svg {...common}>
          <path d="M4 19V9" />
          <path d="M10 19V5" />
          <path d="M16 19v-8" />
          <path d="M21 19v-4" />
        </svg>
      )
    case 'users':
      return (
        <svg {...common}>
          <circle cx="12" y="8" r="3.2" />
          <path d="M5 19a7 7 0 0 1 14 0" />
        </svg>
      )
    case 'profile':
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="3.2" />
          <path d="M6 19a6 6 0 0 1 12 0" />
        </svg>
      )
    default:
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="3" />
        </svg>
      )
  }
}

function itemClass(isActive: boolean) {
  return isActive ? 'nav-link active' : 'nav-link'
}

export function AppLayout() {
  const { user, logout, hasRole } = useAuth()
  const manager = hasRole('ADMIN', 'TRANSPORT_MANAGER')
  const driver = hasRole('DRIVER') && !manager

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">ST</span>
          <div>
            <p className="brand-name">SmartTrans</p>
            <p className="brand-tag">AI Operations</p>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label="Navigation principale">
          <p className="nav-section">Menu</p>
          <NavLink to="/dashboard" className={({ isActive }) => itemClass(isActive)}>
            <Icon name="dashboard" />
            Dashboard
          </NavLink>
          {manager && (
            <>
              <NavLink to="/drivers" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="drivers" />
                Chauffeurs
              </NavLink>
              <NavLink to="/vehicles" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="vehicles" />
                Véhicules
              </NavLink>
              <NavLink to="/clients" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="clients" />
                Clients
              </NavLink>
              <NavLink to="/missions" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="missions" />
                Missions
              </NavLink>
              <NavLink to="/assistant" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="assistant" />
                Assistant
              </NavLink>
              <NavLink to="/activities" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="activities" />
                Activités
              </NavLink>
            </>
          )}
          {driver && (
            <>
              <NavLink to="/missions" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="missions" />
                Mes missions
              </NavLink>
              <NavLink to="/profile" className={({ isActive }) => itemClass(isActive)}>
                <Icon name="profile" />
                Mon profil
              </NavLink>
            </>
          )}

          <p className="nav-section">Général</p>
          {hasRole('ADMIN') && (
            <NavLink to="/users" className={({ isActive }) => itemClass(isActive)}>
              <Icon name="users" />
              Utilisateurs
            </NavLink>
          )}
          <button type="button" className="nav-link nav-logout" onClick={logout}>
            <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
              <path d="M9 6H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3" />
              <path d="M15 8l4 4-4 4" />
              <path d="M10 12h9" />
            </svg>
            Déconnexion
          </button>
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <p className="user-name">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="user-meta">{user?.email}</p>
          </div>
        </div>
      </aside>

      <div className="main-pane">
        <header className="topbar">
          <h1 className="topbar-title">Espace connecté</h1>
          <p className="topbar-email">{user?.email}</p>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

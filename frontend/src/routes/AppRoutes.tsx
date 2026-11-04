import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { PublicOnlyRoute } from '../components/PublicOnlyRoute'
import { AppLayout } from '../layouts/AppLayout'
import { DashboardPage } from '../pages/DashboardPage'
import { ClientCreatePage } from '../pages/clients/ClientCreatePage'
import { ClientDetailPage } from '../pages/clients/ClientDetailPage'
import { ClientEditPage } from '../pages/clients/ClientEditPage'
import { ClientsListPage } from '../pages/clients/ClientsListPage'
import { ActivitiesListPage } from '../pages/activities/ActivitiesListPage'
import { ActivityDetailPage } from '../pages/activities/ActivityDetailPage'
import { DriverCreatePage } from '../pages/drivers/DriverCreatePage'
import { DriverDetailPage } from '../pages/drivers/DriverDetailPage'
import { DriverEditPage } from '../pages/drivers/DriverEditPage'
import { DriversListPage } from '../pages/drivers/DriversListPage'
import { AssistantPage } from '../pages/assistant/AssistantPage'
import { LoginPage } from '../pages/LoginPage'
import { DriverProfilePage } from '../pages/profile/DriverProfilePage'
import { MissionCreatePage } from '../pages/missions/MissionCreatePage'
import { MissionDetailPage } from '../pages/missions/MissionDetailPage'
import { MissionEditPage } from '../pages/missions/MissionEditPage'
import { MissionsListPage } from '../pages/missions/MissionsListPage'
import { RolesListPage } from '../pages/users/RolesListPage'
import { UserCreatePage } from '../pages/users/UserCreatePage'
import { UserDetailPage } from '../pages/users/UserDetailPage'
import { UserEditPage } from '../pages/users/UserEditPage'
import { UsersListPage } from '../pages/users/UsersListPage'
import { VehicleCreatePage } from '../pages/vehicles/VehicleCreatePage'
import { VehicleDetailPage } from '../pages/vehicles/VehicleDetailPage'
import { VehicleEditPage } from '../pages/vehicles/VehicleEditPage'
import { VehiclesListPage } from '../pages/vehicles/VehiclesListPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />

          <Route path="/missions" element={<MissionsListPage />} />

          <Route element={<ProtectedRoute roles={['ADMIN', 'TRANSPORT_MANAGER']} />}>
            <Route path="/drivers" element={<DriversListPage />} />
            <Route path="/drivers/new" element={<DriverCreatePage />} />
            <Route path="/drivers/:id/edit" element={<DriverEditPage />} />

            <Route path="/vehicles" element={<VehiclesListPage />} />
            <Route path="/vehicles/new" element={<VehicleCreatePage />} />
            <Route path="/vehicles/:id/edit" element={<VehicleEditPage />} />

            <Route path="/clients" element={<ClientsListPage />} />
            <Route path="/clients/new" element={<ClientCreatePage />} />
            <Route path="/clients/:id/edit" element={<ClientEditPage />} />
            <Route path="/clients/:id" element={<ClientDetailPage />} />

            <Route path="/missions/new" element={<MissionCreatePage />} />
            <Route path="/missions/:id/edit" element={<MissionEditPage />} />

            <Route path="/assistant" element={<AssistantPage />} />

            <Route path="/activities" element={<ActivitiesListPage />} />
            <Route path="/activities/:id" element={<ActivityDetailPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['ADMIN']} />}>
            <Route path="/users" element={<UsersListPage />} />
            <Route path="/users/new" element={<UserCreatePage />} />
            <Route path="/users/:id/edit" element={<UserEditPage />} />
            <Route path="/users/:id" element={<UserDetailPage />} />
            <Route path="/roles" element={<RolesListPage />} />
          </Route>

          <Route path="/drivers/:id" element={<DriverDetailPage />} />
          <Route path="/vehicles/:id" element={<VehicleDetailPage />} />
          <Route path="/missions/:id" element={<MissionDetailPage />} />

          <Route element={<ProtectedRoute roles={['DRIVER']} />}>
            <Route path="/profile" element={<DriverProfilePage />} />
          </Route>
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

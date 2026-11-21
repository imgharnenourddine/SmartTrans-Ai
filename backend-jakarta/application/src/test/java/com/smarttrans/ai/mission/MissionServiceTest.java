package com.smarttrans.ai.mission;

import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.activity.ActivityLog;
import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.Forbidden;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.ActivityAction;
import com.smarttrans.ai.domain.Client;
import com.smarttrans.ai.domain.Driver;
import com.smarttrans.ai.domain.DriverStatus;
import com.smarttrans.ai.domain.Mission;
import com.smarttrans.ai.domain.MissionStatus;
import com.smarttrans.ai.domain.RoleCode;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.domain.Vehicle;
import com.smarttrans.ai.domain.VehicleStatus;
import com.smarttrans.ai.domain.VehicleType;
import com.smarttrans.ai.mission.MissionService.CreateMissionRequest;
import com.smarttrans.ai.mission.MissionService.StatusRequest;
import com.smarttrans.ai.security.CurrentUser;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ActivityLog activityLog = mock(ActivityLog.class);
    private final CurrentUser currentUser = new CurrentUser();
    private MissionService missions;

    @BeforeEach
    void setUp() {
        missions = new MissionService();
        ServiceTestSupport.inject(missions, entityManager, activityLog, currentUser);
        ServiceTestSupport.stubQueries(entityManager, Map.of());
        currentUser.authenticate(UUID.randomUUID(), "admin@smarttrans.ai", Set.of(RoleCode.ADMIN));
    }

    @Test
    void createUppercasesReferenceAndTrimsText() {
        UUID clientId = UUID.randomUUID();
        when(entityManager.find(Client.class, clientId)).thenReturn(client(clientId, "Atlas Freight"));

        var response = missions.create(new CreateMissionRequest(
                "  db-10  ",
                clientId,
                null,
                null,
                null,
                "  Rabat  ",
                "  Tanger  ",
                Instant.parse("2026-12-15T09:30:00Z"),
                new BigDecimal("800"),
                "  Colis  ",
                "   ",
                null));

        assertEquals("DB-10", response.reference());
        assertEquals("Atlas Freight", response.clientName());
        assertEquals("Rabat", response.departureLocation());
        assertEquals("Tanger", response.destination());
        assertEquals("Colis", response.cargoType());
        assertNull(response.observations());
        assertEquals(MissionStatus.PLANIFIED, response.status());
        verify(activityLog).record(
                eq(ActivityAction.CREATE),
                eq("MISSION"),
                isNull(),
                eq(ActivityLog.metadata("reference", "DB-10", "status", "PLANIFIED")));
    }

    @Test
    void rejectsDuplicateReference() {
        ServiceTestSupport.stubQueries(entityManager, Map.of("m.reference", 1L));

        Conflict error = assertThrows(Conflict.class, () -> missions.create(createRequest(UUID.randomUUID(), "db-10")));

        assertEquals("Mission reference already in use: DB-10", error.getMessage());
    }

    @Test
    void missingClientIsABusinessError() {
        UUID clientId = UUID.randomUUID();

        Business error = assertThrows(Business.class, () -> missions.create(createRequest(clientId, "db-10")));

        assertEquals("Client not found with id: " + clientId, error.getMessage());
    }

    @Test
    void rejectsAnInvalidStatusTransition() {
        UUID id = UUID.randomUUID();
        when(entityManager.find(Mission.class, id)).thenReturn(plannedMission());

        Conflict error = assertThrows(Conflict.class, () -> missions.updateStatus(id, new StatusRequest(MissionStatus.COMPLETED)));

        assertEquals("Invalid status transition: PLANIFIED -> COMPLETED", error.getMessage());
    }

    @Test
    void startRequiresAnAssignedDriverAndVehicle() {
        UUID id = UUID.randomUUID();
        Mission mission = plannedMission();
        when(entityManager.find(Mission.class, id)).thenReturn(mission);

        Business error = assertThrows(Business.class, () -> missions.updateStatus(id, new StatusRequest(MissionStatus.IN_PROGRESS)));

        assertEquals("A mission requires an assigned driver and vehicle to start", error.getMessage());
    }

    @Test
    void startAssignsResourcesAndCancelReleasesThem() {
        UUID id = UUID.randomUUID();
        Driver driver = availableDriver();
        Vehicle vehicle = availableVehicle();
        Mission mission = plannedMission();
        mission.setDriver(driver);
        mission.setVehicle(vehicle);
        when(entityManager.find(Mission.class, id)).thenReturn(mission);

        var started = missions.updateStatus(id, new StatusRequest(MissionStatus.IN_PROGRESS));

        assertEquals(MissionStatus.IN_PROGRESS, started.status());
        assertNotNull(started.startedAt());
        assertEquals(DriverStatus.ON_MISSION, driver.getStatus());
        assertEquals(VehicleStatus.IN_USE, vehicle.getStatus());

        var cancelled = missions.updateStatus(id, new StatusRequest(MissionStatus.CANCELLED));

        assertEquals(MissionStatus.CANCELLED, cancelled.status());
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        verify(activityLog).record(
                eq(ActivityAction.STATUS_CHANGE),
                eq("MISSION"),
                isNull(),
                eq(ActivityLog.metadata("reference", "DB-10", "from", "PLANIFIED", "to", "IN_PROGRESS")));
    }

    @Test
    void rejectsStartWhenInsuranceIsExpired() {
        UUID id = UUID.randomUUID();
        Vehicle vehicle = availableVehicle();
        vehicle.setInsuranceExpiresAt(LocalDate.now(ZoneOffset.UTC).minusDays(1));
        Mission mission = plannedMission();
        mission.setDriver(availableDriver());
        mission.setVehicle(vehicle);
        when(entityManager.find(Mission.class, id)).thenReturn(mission);

        Business error = assertThrows(Business.class, () -> missions.updateStatus(id, new StatusRequest(MissionStatus.IN_PROGRESS)));

        assertEquals("Vehicle insurance is expired: DB-702-A", error.getMessage());
        assertEquals(DriverStatus.AVAILABLE, mission.getDriver().getStatus());
        assertEquals(VehicleStatus.AVAILABLE, mission.getVehicle().getStatus());
    }

    @Test
    void rejectsDeleteOfAMissionInProgress() {
        UUID id = UUID.randomUUID();
        Mission mission = plannedMission();
        mission.setStatus(MissionStatus.IN_PROGRESS);
        when(entityManager.find(Mission.class, id)).thenReturn(mission);

        Conflict error = assertThrows(Conflict.class, () -> missions.delete(id));

        assertEquals("Only planned or cancelled missions can be deleted: DB-10", error.getMessage());
    }

    @Test
    void assignedDriverCannotCancel() {
        UUID driverUserId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        ServiceTestSupport.setId(user, driverUserId);
        Driver driver = availableDriver();
        driver.setUser(user);
        Mission mission = plannedMission();
        mission.setDriver(driver);
        UUID id = UUID.randomUUID();
        when(entityManager.find(Mission.class, id)).thenReturn(mission);
        currentUser.authenticate(driverUserId, "driver@smarttrans.ai", Set.of(RoleCode.DRIVER));

        Forbidden error = assertThrows(Forbidden.class, () -> missions.updateStatus(id, new StatusRequest(MissionStatus.CANCELLED)));

        assertEquals("Access denied", error.getMessage());
        assertEquals(MissionStatus.PLANIFIED, mission.getStatus());
    }

    @Test
    void driverCannotReadAnotherDriversMission() {
        UUID otherUserId = UUID.randomUUID();
        UserAccount other = new UserAccount();
        ServiceTestSupport.setId(other, otherUserId);
        Driver driver = availableDriver();
        driver.setUser(other);
        Mission mission = plannedMission();
        mission.setDriver(driver);
        UUID id = UUID.randomUUID();
        when(entityManager.find(Mission.class, id)).thenReturn(mission);
        currentUser.authenticate(UUID.randomUUID(), "driver@smarttrans.ai", Set.of(RoleCode.DRIVER));

        Forbidden error = assertThrows(Forbidden.class, () -> missions.getById(id));

        assertEquals("Access denied", error.getMessage());
    }

    @Test
    void driverWithoutAProfileCannotListMissions() {
        currentUser.authenticate(UUID.randomUUID(), "driver@smarttrans.ai", Set.of(RoleCode.DRIVER));

        NotFound error = assertThrows(NotFound.class, missions::getAll);

        assertEquals("Driver profile not found for current user", error.getMessage());
    }

    private static CreateMissionRequest createRequest(UUID clientId, String reference) {
        return new CreateMissionRequest(
                reference,
                clientId,
                null,
                null,
                null,
                "Rabat",
                "Tanger",
                Instant.parse("2026-12-15T09:30:00Z"),
                new BigDecimal("800"),
                null,
                null,
                null);
    }

    private static Client client(UUID id, String company) {
        Client client = new Client();
        ServiceTestSupport.setId(client, id);
        client.setCompanyName(company);
        client.setAddress("Rabat");
        client.setPhone("0600");
        return client;
    }

    private static Mission plannedMission() {
        Mission mission = new Mission();
        mission.setReference("DB-10");
        mission.setClient(client(UUID.randomUUID(), "Atlas Freight"));
        mission.setDepartureLocation("Rabat");
        mission.setDestination("Tanger");
        mission.setScheduledAt(Instant.parse("2026-12-15T09:30:00Z"));
        mission.setStatus(MissionStatus.PLANIFIED);
        return mission;
    }

    private static Driver availableDriver() {
        Driver driver = new Driver();
        driver.setFirstName("Sara");
        driver.setLastName("Bennani");
        driver.setLicenseNumber("LIC-1");
        driver.setLicenseExpiresAt(LocalDate.now(ZoneOffset.UTC).plusYears(1));
        driver.setStatus(DriverStatus.AVAILABLE);
        return driver;
    }

    private static Vehicle availableVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("DB-702-A");
        vehicle.setType(VehicleType.VAN);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setInsuranceExpiresAt(LocalDate.now(ZoneOffset.UTC).plusYears(1));
        vehicle.setTechnicalVisitExpiresAt(LocalDate.now(ZoneOffset.UTC).plusYears(1));
        return vehicle;
    }
}

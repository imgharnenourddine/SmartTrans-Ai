package com.smarttrans.ai.driver;

import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.Forbidden;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Driver;
import com.smarttrans.ai.domain.DriverStatus;
import com.smarttrans.ai.domain.RoleCode;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.driver.DriverService.CreateDriverRequest;
import com.smarttrans.ai.driver.DriverService.UpdateDriverAvailabilityRequest;
import com.smarttrans.ai.driver.DriverService.UpdateDriverRequest;
import com.smarttrans.ai.security.CurrentUser;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DriverServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final CurrentUser currentUser = new CurrentUser();
    private DriverService drivers;

    @BeforeEach
    void setUp() {
        drivers = new DriverService();
        ServiceTestSupport.inject(drivers, entityManager, currentUser);
        ServiceTestSupport.stubQueries(entityManager, Map.of());
        currentUser.authenticate(UUID.randomUUID(), "admin@smarttrans.ai", Set.of(RoleCode.ADMIN));
    }

    @Test
    void createTrimsFieldsAndDefaultsToAvailable() {
        var response = drivers.create(new CreateDriverRequest(
                null,
                "  Sara  ",
                "  Bennani  ",
                "  0600  ",
                "  LIC-1  ",
                LocalDate.now(ZoneOffset.UTC).plusYears(1),
                (short) 4,
                null));

        assertEquals("Sara", response.firstName());
        assertEquals("Bennani", response.lastName());
        assertEquals("LIC-1", response.licenseNumber());
        assertEquals(DriverStatus.AVAILABLE, response.status());
        assertTrue(response.availableForMission());
        assertFalse(response.licenseExpired());
    }

    @Test
    void rejectsDuplicateLicense() {
        ServiceTestSupport.stubQueries(entityManager, Map.of("d.licenseNumber", 1L));

        Conflict error = assertThrows(Conflict.class, () -> drivers.create(request(null)));

        assertEquals("License number already in use: LIC-1", error.getMessage());
    }

    @Test
    void rejectsUserAlreadyLinkedToADriver() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        ServiceTestSupport.setId(user, userId);
        when(entityManager.find(UserAccount.class, userId)).thenReturn(user);
        ServiceTestSupport.stubQueries(entityManager, Map.of("d.user.id", 1L));

        Conflict error = assertThrows(Conflict.class, () -> drivers.create(request(userId)));

        assertEquals("User already linked to a driver: " + userId, error.getMessage());
    }

    @Test
    void rejectsUnknownLinkedUser() {
        UUID userId = UUID.randomUUID();

        Business error = assertThrows(Business.class, () -> drivers.create(request(userId)));

        assertEquals("User not found with id: " + userId, error.getMessage());
    }

    @Test
    void rejectsSuspendWhileOnMission() {
        UUID id = UUID.randomUUID();
        when(entityManager.find(Driver.class, id)).thenReturn(driver(DriverStatus.ON_MISSION, tomorrow()));

        Conflict error = assertThrows(Conflict.class, () -> drivers.update(id, statusOnly(DriverStatus.SUSPENDED)));

        assertEquals("Cannot suspend a driver currently on mission", error.getMessage());
    }

    @Test
    void availabilityRejectsStatusesOutsideAvailableAndUnavailable() {
        UUID id = UUID.randomUUID();
        when(entityManager.find(Driver.class, id)).thenReturn(driver(DriverStatus.AVAILABLE, tomorrow()));

        Business error = assertThrows(Business.class,
                () -> drivers.updateAvailability(id, new UpdateDriverAvailabilityRequest(DriverStatus.ON_MISSION)));

        assertEquals("Availability can only be set to AVAILABLE or UNAVAILABLE", error.getMessage());
    }

    @Test
    void availabilityCannotChangeWhileOnMissionOrSuspendedOrExpired() {
        UUID onMission = UUID.randomUUID();
        when(entityManager.find(Driver.class, onMission)).thenReturn(driver(DriverStatus.ON_MISSION, tomorrow()));
        Conflict onMissionError = assertThrows(Conflict.class,
                () -> drivers.updateAvailability(onMission, available()));
        assertEquals("Cannot change availability while driver is on mission", onMissionError.getMessage());

        UUID suspended = UUID.randomUUID();
        when(entityManager.find(Driver.class, suspended)).thenReturn(driver(DriverStatus.SUSPENDED, tomorrow()));
        Conflict suspendedError = assertThrows(Conflict.class,
                () -> drivers.updateAvailability(suspended, available()));
        assertEquals("Suspended driver availability cannot be changed from this endpoint", suspendedError.getMessage());

        UUID expired = UUID.randomUUID();
        when(entityManager.find(Driver.class, expired))
                .thenReturn(driver(DriverStatus.UNAVAILABLE, LocalDate.now(ZoneOffset.UTC).minusDays(1)));
        Business expiredError = assertThrows(Business.class,
                () -> drivers.updateAvailability(expired, available()));
        assertEquals("Cannot set driver as available: driving license is expired", expiredError.getMessage());
    }

    @Test
    void rejectsNegativeExpiringWindow() {
        Business error = assertThrows(Business.class, () -> drivers.findExpiringLicenses(-1));

        assertEquals("withinDays must be zero or positive", error.getMessage());
    }

    @Test
    void rejectsDeactivateWhileOnMission() {
        UUID id = UUID.randomUUID();
        when(entityManager.find(Driver.class, id)).thenReturn(driver(DriverStatus.ON_MISSION, tomorrow()));

        Conflict error = assertThrows(Conflict.class, () -> drivers.deactivate(id));

        assertEquals("Cannot deactivate a driver currently on mission", error.getMessage());
    }

    @Test
    void driverCannotReadAnUnlinkedProfile() {
        UUID id = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UserAccount other = new UserAccount();
        ServiceTestSupport.setId(other, otherUserId);
        Driver driver = driver(DriverStatus.AVAILABLE, tomorrow());
        driver.setUser(other);
        when(entityManager.find(Driver.class, id)).thenReturn(driver);
        currentUser.authenticate(UUID.randomUUID(), "driver@smarttrans.ai", Set.of(RoleCode.DRIVER));

        Forbidden error = assertThrows(Forbidden.class, () -> drivers.getById(id));

        assertEquals("Access denied", error.getMessage());
    }

    @Test
    void missingProfileForCurrentUser() {
        currentUser.authenticate(UUID.randomUUID(), "driver@smarttrans.ai", Set.of(RoleCode.DRIVER));

        NotFound error = assertThrows(NotFound.class, drivers::getMe);

        assertEquals("Driver profile not found for current user", error.getMessage());
    }

    private static CreateDriverRequest request(UUID userId) {
        return new CreateDriverRequest(userId, "Sara", "Bennani", "0600", "LIC-1", tomorrow(), (short) 4, null);
    }

    private static UpdateDriverRequest statusOnly(DriverStatus status) {
        return new UpdateDriverRequest(null, null, null, null, null, null, null, status);
    }

    private static UpdateDriverAvailabilityRequest available() {
        return new UpdateDriverAvailabilityRequest(DriverStatus.AVAILABLE);
    }

    private static Driver driver(DriverStatus status, LocalDate licenseExpiresAt) {
        Driver driver = new Driver();
        driver.setFirstName("Sara");
        driver.setLastName("Bennani");
        driver.setPhone("0600");
        driver.setLicenseNumber("LIC-1");
        driver.setLicenseExpiresAt(licenseExpiresAt);
        driver.setStatus(status);
        return driver;
    }

    private static LocalDate tomorrow() {
        return LocalDate.now(ZoneOffset.UTC).plusDays(1);
    }
}

package com.smarttrans.ai.vehicle;

import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Vehicle;
import com.smarttrans.ai.domain.VehicleStatus;
import com.smarttrans.ai.domain.VehicleType;
import com.smarttrans.ai.vehicle.VehicleService.CreateVehicleRequest;
import com.smarttrans.ai.vehicle.VehicleService.UpdateVehicleRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private VehicleService vehicles;

    @BeforeEach
    void setUp() {
        vehicles = new VehicleService();
        ServiceTestSupport.inject(vehicles, entityManager);
        ServiceTestSupport.stubQueries(entityManager, Map.of());
    }

    @Test
    void createNormalizesRegistrationAndDefaultsMileageAndStatus() {
        var response = vehicles.create(new CreateVehicleRequest(
                "  db-702-a  ",
                VehicleType.VAN,
                new BigDecimal("1200"),
                null,
                null,
                null,
                null,
                null));

        assertEquals("DB-702-A", response.registrationNumber());
        assertEquals(0, response.mileageKm());
        assertEquals(VehicleStatus.AVAILABLE, response.status());
        assertEquals(VehicleType.VAN, response.type());
    }

    @Test
    void rejectsDuplicateRegistration() {
        ServiceTestSupport.stubQueries(entityManager, Map.of("v.registrationNumber", 1L));

        Conflict error = assertThrows(Conflict.class, () -> vehicles.create(request("db-702-a")));

        assertEquals("Registration number already in use: DB-702-A", error.getMessage());
    }

    @Test
    void rejectsDeactivateAndOutOfServiceWhileInUse() {
        UUID id = UUID.randomUUID();
        Vehicle vehicle = vehicle(VehicleStatus.IN_USE);
        when(entityManager.find(Vehicle.class, id)).thenReturn(vehicle);

        Conflict deactivate = assertThrows(Conflict.class, () -> vehicles.deactivate(id));
        assertEquals("Cannot deactivate a vehicle currently in use on a mission", deactivate.getMessage());
        assertEquals(VehicleStatus.IN_USE, vehicle.getStatus());

        Conflict update = assertThrows(Conflict.class, () -> vehicles.update(id, statusOnly(VehicleStatus.OUT_OF_SERVICE)));
        assertEquals("Cannot deactivate a vehicle currently in use on a mission", update.getMessage());
        assertEquals(VehicleStatus.IN_USE, vehicle.getStatus());
    }

    @Test
    void deactivateMarksAnAvailableVehicleOutOfService() {
        UUID id = UUID.randomUUID();
        when(entityManager.find(Vehicle.class, id)).thenReturn(vehicle(VehicleStatus.AVAILABLE));

        var response = vehicles.deactivate(id);

        assertEquals(VehicleStatus.OUT_OF_SERVICE, response.status());
    }

    @Test
    void rejectsNegativeExpiringWindow() {
        Business error = assertThrows(Business.class, () -> vehicles.findWithExpiringDocuments(-1));

        assertEquals("withinDays must be zero or positive", error.getMessage());
    }

    @Test
    void missingVehicleIsNotFound() {
        UUID id = UUID.randomUUID();

        NotFound error = assertThrows(NotFound.class, () -> vehicles.getById(id));

        assertEquals("Vehicle not found with id: " + id, error.getMessage());
    }

    private static CreateVehicleRequest request(String registration) {
        return new CreateVehicleRequest(registration, VehicleType.VAN, new BigDecimal("1200"), 0, null, null, null, null);
    }

    private static UpdateVehicleRequest statusOnly(VehicleStatus status) {
        return new UpdateVehicleRequest(null, null, null, null, null, null, null, status);
    }

    private static Vehicle vehicle(VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("DB-702-A");
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacityKg(new BigDecimal("1200"));
        vehicle.setMileageKm(42000);
        vehicle.setStatus(status);
        return vehicle;
    }
}

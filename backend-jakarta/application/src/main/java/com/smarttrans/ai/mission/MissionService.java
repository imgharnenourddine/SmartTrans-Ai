package com.smarttrans.ai.mission;

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
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.domain.Vehicle;
import com.smarttrans.ai.domain.VehicleStatus;
import com.smarttrans.ai.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MissionService {

    private static final Map<MissionStatus, Set<MissionStatus>> ALLOWED = Map.of(
            MissionStatus.PLANIFIED, Set.of(MissionStatus.IN_PROGRESS, MissionStatus.CANCELLED),
            MissionStatus.IN_PROGRESS, Set.of(MissionStatus.COMPLETED, MissionStatus.CANCELLED),
            MissionStatus.COMPLETED, Set.of(),
            MissionStatus.CANCELLED, Set.of());

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Inject
    private CurrentUser currentUser;

    @Inject
    private ActivityLog activityLog;

    public MissionResponse create(CreateMissionRequest request) {
        String reference = request.reference().trim().toUpperCase();
        if (existsByReference(reference)) {
            throw new Conflict("Mission reference already in use: " + reference);
        }
        Client client = findClient(request.clientId());
        Driver driver = resolveDriver(request.driverId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        UserAccount createdBy = resolveUser(request.createdById());
        if (createdBy == null && currentUser.isAuthenticated()) {
            createdBy = entityManager.find(UserAccount.class, currentUser.id());
        }
        if (request.status() != null && request.status() != MissionStatus.PLANIFIED) {
            validateTransition(MissionStatus.PLANIFIED, request.status());
        }
        Mission mission = new Mission();
        mission.setReference(reference);
        mission.setClient(client);
        mission.setDriver(driver);
        mission.setVehicle(vehicle);
        mission.setCreatedBy(createdBy);
        mission.setDepartureLocation(request.departureLocation().trim());
        mission.setDestination(request.destination().trim());
        mission.setScheduledAt(request.scheduledAt());
        mission.setWeightKg(request.weightKg());
        mission.setCargoType(trimToNull(request.cargoType()));
        mission.setObservations(trimToNull(request.observations()));
        mission.setStatus(request.status() != null ? request.status() : MissionStatus.PLANIFIED);
        applyStatusTimestamps(mission, mission.getStatus());
        if (mission.getStatus() == MissionStatus.IN_PROGRESS) {
            startMissionResources(mission);
        }
        entityManager.persist(mission);
        entityManager.flush();
        activityLog.record(ActivityAction.CREATE, "MISSION", mission.getId(),
                ActivityLog.metadata("reference", mission.getReference(), "status", mission.getStatus().name()));
        return toResponse(mission);
    }

    public MissionResponse getById(UUID id) {
        Mission mission = find(id);
        assertCanRead(mission);
        return toResponse(mission);
    }

    public List<MissionResponse> getAll() {
        UUID driverId = staff() ? null : requireCurrentDriverId();
        String query = driverId == null
                ? "select m from Mission m order by m.scheduledAt desc, m.reference asc"
                : "select m from Mission m where m.driver.id = :driverId order by m.scheduledAt desc, m.reference asc";
        var typed = entityManager.createQuery(query, Mission.class);
        if (driverId != null) {
            typed.setParameter("driverId", driverId);
        }
        return typed.getResultList().stream().map(this::toResponse).toList();
    }

    public MissionResponse update(UUID id, UpdateMissionRequest request) {
        if (!staff()) {
            throw new Forbidden();
        }
        Mission mission = find(id);
        if (request.reference() != null) {
            String reference = request.reference().trim().toUpperCase();
            if (!reference.equalsIgnoreCase(mission.getReference()) && existsByReference(reference)) {
                throw new Conflict("Mission reference already in use: " + reference);
            }
        }
        boolean updateDriver = request.driverId() != null;
        Driver driver = updateDriver ? resolveDriver(request.driverId()) : null;
        boolean updateVehicle = request.vehicleId() != null;
        Vehicle vehicle = updateVehicle ? resolveVehicle(request.vehicleId()) : null;
        assertNotStartedForReassignment(mission, updateDriver, updateVehicle, driver, vehicle);
        MissionStatus previous = mission.getStatus();
        if (request.reference() != null) {
            mission.setReference(request.reference().trim().toUpperCase());
        }
        if (request.clientId() != null) {
            mission.setClient(findClient(request.clientId()));
        }
        if (updateDriver) {
            mission.setDriver(driver);
        }
        if (updateVehicle) {
            mission.setVehicle(vehicle);
        }
        if (request.departureLocation() != null) {
            mission.setDepartureLocation(request.departureLocation().trim());
        }
        if (request.destination() != null) {
            mission.setDestination(request.destination().trim());
        }
        if (request.scheduledAt() != null) {
            mission.setScheduledAt(request.scheduledAt());
        }
        if (request.startedAt() != null) {
            mission.setStartedAt(request.startedAt());
        }
        if (request.completedAt() != null) {
            mission.setCompletedAt(request.completedAt());
        }
        if (request.weightKg() != null) {
            mission.setWeightKg(request.weightKg());
        }
        if (request.cargoType() != null) {
            mission.setCargoType(trimToNull(request.cargoType()));
        }
        if (request.observations() != null) {
            mission.setObservations(trimToNull(request.observations()));
        }
        if (request.status() != null && request.status() != previous) {
            transition(mission, request.status());
        }
        recordChange(mission, previous);
        return toResponse(mission);
    }

    public MissionResponse updateStatus(UUID id, StatusRequest request) {
        Mission mission = find(id);
        assertCanManage(mission);
        if (currentUser.hasRole("DRIVER") && !staff() && request.status() == MissionStatus.CANCELLED) {
            throw new Forbidden();
        }
        MissionStatus previous = mission.getStatus();
        transition(mission, request.status());
        recordChange(mission, previous);
        return toResponse(mission);
    }

    public void delete(UUID id) {
        Mission mission = find(id);
        if (mission.getStatus() == MissionStatus.IN_PROGRESS || mission.getStatus() == MissionStatus.COMPLETED) {
            throw new Conflict("Only planned or cancelled missions can be deleted: " + mission.getReference());
        }
        activityLog.record(ActivityAction.DELETE, "MISSION", mission.getId(),
                ActivityLog.metadata("reference", mission.getReference()));
        entityManager.remove(mission);
    }

    private void recordChange(Mission mission, MissionStatus previous) {
        if (mission.getStatus() != previous) {
            activityLog.record(ActivityAction.STATUS_CHANGE, "MISSION", mission.getId(), ActivityLog.metadata(
                    "reference", mission.getReference(),
                    "from", previous.name(),
                    "to", mission.getStatus().name()));
            return;
        }
        activityLog.record(ActivityAction.UPDATE, "MISSION", mission.getId(),
                ActivityLog.metadata("reference", mission.getReference()));
    }

    private void transition(Mission mission, MissionStatus target) {
        validateTransition(mission.getStatus(), target);
        applyStatusTimestamps(mission, target);
        mission.setStatus(target);
        if (target == MissionStatus.IN_PROGRESS) {
            startMissionResources(mission);
        }
        if (target == MissionStatus.COMPLETED || target == MissionStatus.CANCELLED) {
            releaseMissionResources(mission);
        }
    }

    private void validateTransition(MissionStatus current, MissionStatus target) {
        if (!ALLOWED.get(current).contains(target)) {
            throw new Conflict("Invalid status transition: " + current + " -> " + target);
        }
    }

    private void startMissionResources(Mission mission) {
        if (mission.getDriver() == null || mission.getVehicle() == null) {
            throw new Business("A mission requires an assigned driver and vehicle to start");
        }
        Driver driver = mission.getDriver();
        Vehicle vehicle = mission.getVehicle();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new Business("Driver is not available: " + driver.getFirstName() + " " + driver.getLastName());
        }
        if (driver.getLicenseExpiresAt().isBefore(today)) {
            throw new Business("Driver license is expired: " + driver.getFirstName() + " " + driver.getLastName());
        }
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new Business("Vehicle is not available: " + vehicle.getRegistrationNumber());
        }
        if (vehicle.getInsuranceExpiresAt() != null && vehicle.getInsuranceExpiresAt().isBefore(today)) {
            throw new Business("Vehicle insurance is expired: " + vehicle.getRegistrationNumber());
        }
        if (vehicle.getTechnicalVisitExpiresAt() != null && vehicle.getTechnicalVisitExpiresAt().isBefore(today)) {
            throw new Business("Vehicle technical visit is expired: " + vehicle.getRegistrationNumber());
        }
        driver.setStatus(DriverStatus.ON_MISSION);
        vehicle.setStatus(VehicleStatus.IN_USE);
    }

    private void releaseMissionResources(Mission mission) {
        if (mission.getDriver() != null && mission.getDriver().getStatus() == DriverStatus.ON_MISSION) {
            mission.getDriver().setStatus(DriverStatus.AVAILABLE);
        }
        if (mission.getVehicle() != null && mission.getVehicle().getStatus() == VehicleStatus.IN_USE) {
            mission.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }
    }

    private void assertNotStartedForReassignment(
            Mission mission, boolean updateDriver, boolean updateVehicle, Driver driver, Vehicle vehicle) {
        if (mission.getStatus() != MissionStatus.PLANIFIED) {
            if (updateDriver && driver != mission.getDriver()) {
                throw new Conflict("Driver can only be reassigned while the mission is planned: " + mission.getReference());
            }
            if (updateVehicle && vehicle != mission.getVehicle()) {
                throw new Conflict("Vehicle can only be reassigned while the mission is planned: " + mission.getReference());
            }
            return;
        }
        Driver effectiveDriver = updateDriver ? driver : mission.getDriver();
        Vehicle effectiveVehicle = updateVehicle ? vehicle : mission.getVehicle();
        if (effectiveDriver != null && effectiveDriver.getStatus() == DriverStatus.ON_MISSION) {
            throw new Conflict("Driver is already on another mission: "
                    + effectiveDriver.getFirstName() + " " + effectiveDriver.getLastName());
        }
        if (effectiveVehicle != null && effectiveVehicle.getStatus() == VehicleStatus.IN_USE) {
            throw new Conflict("Vehicle is already in use on another mission: " + effectiveVehicle.getRegistrationNumber());
        }
    }

    private void applyStatusTimestamps(Mission mission, MissionStatus status) {
        Instant now = Instant.now();
        if (status == MissionStatus.IN_PROGRESS && mission.getStartedAt() == null) {
            mission.setStartedAt(now);
        }
        if (status == MissionStatus.COMPLETED && mission.getCompletedAt() == null) {
            if (mission.getStartedAt() == null) {
                mission.setStartedAt(now);
            }
            mission.setCompletedAt(now);
        }
    }

    private void assertCanRead(Mission mission) {
        if (staff()) {
            return;
        }
        if (currentUser.hasRole("DRIVER") && assignedToCurrentUser(mission)) {
            return;
        }
        throw new Forbidden();
    }

    private void assertCanManage(Mission mission) {
        if (staff()) {
            return;
        }
        if (currentUser.hasRole("DRIVER") && assignedToCurrentUser(mission)) {
            return;
        }
        throw new Forbidden();
    }

    private boolean assignedToCurrentUser(Mission mission) {
        return mission.getDriver() != null
                && mission.getDriver().getUser() != null
                && mission.getDriver().getUser().getId().equals(currentUser.id());
    }

    private boolean staff() {
        return currentUser.hasAny("ADMIN", "TRANSPORT_MANAGER");
    }

    private UUID requireCurrentDriverId() {
        List<Driver> drivers = entityManager.createQuery(
                        "select d from Driver d where d.user.id = :userId", Driver.class)
                .setParameter("userId", currentUser.id())
                .getResultList();
        if (drivers.isEmpty()) {
            throw new NotFound("Driver profile not found for current user");
        }
        return drivers.getFirst().getId();
    }

    private Mission find(UUID id) {
        Mission mission = entityManager.find(Mission.class, id);
        if (mission == null) {
            throw new NotFound("Mission not found with id: " + id);
        }
        return mission;
    }

    private Client findClient(UUID id) {
        Client client = entityManager.find(Client.class, id);
        if (client == null) {
            throw new Business("Client not found with id: " + id);
        }
        return client;
    }

    private Driver resolveDriver(UUID id) {
        if (id == null) {
            return null;
        }
        Driver driver = entityManager.find(Driver.class, id);
        if (driver == null) {
            throw new Business("Driver not found with id: " + id);
        }
        return driver;
    }

    private Vehicle resolveVehicle(UUID id) {
        if (id == null) {
            return null;
        }
        Vehicle vehicle = entityManager.find(Vehicle.class, id);
        if (vehicle == null) {
            throw new Business("Vehicle not found with id: " + id);
        }
        return vehicle;
    }

    private UserAccount resolveUser(UUID id) {
        if (id == null) {
            return null;
        }
        UserAccount user = entityManager.find(UserAccount.class, id);
        if (user == null) {
            throw new Business("User not found with id: " + id);
        }
        return user;
    }

    private boolean existsByReference(String reference) {
        Long count = entityManager.createQuery(
                        "select count(m) from Mission m where m.reference = :reference", Long.class)
                .setParameter("reference", reference)
                .getSingleResult();
        return count > 0;
    }

    private MissionResponse toResponse(Mission mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getReference(),
                mission.getClient() != null ? mission.getClient().getId() : null,
                mission.getClient() != null ? mission.getClient().getCompanyName() : null,
                mission.getDriver() != null ? mission.getDriver().getId() : null,
                mission.getDriver() != null ? mission.getDriver().getFirstName() : null,
                mission.getDriver() != null ? mission.getDriver().getLastName() : null,
                mission.getVehicle() != null ? mission.getVehicle().getId() : null,
                mission.getVehicle() != null ? mission.getVehicle().getRegistrationNumber() : null,
                mission.getCreatedBy() != null ? mission.getCreatedBy().getId() : null,
                mission.getDepartureLocation(),
                mission.getDestination(),
                mission.getScheduledAt(),
                mission.getStartedAt(),
                mission.getCompletedAt(),
                mission.getWeightKg(),
                mission.getCargoType(),
                mission.getObservations(),
                mission.getStatus(),
                mission.getCreatedAt(),
                mission.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record CreateMissionRequest(
            @NotBlank @Size(max = 30) String reference,
            @NotNull UUID clientId,
            UUID driverId,
            UUID vehicleId,
            UUID createdById,
            @NotBlank @Size(max = 255) String departureLocation,
            @NotBlank @Size(max = 255) String destination,
            @NotNull Instant scheduledAt,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
            @Size(max = 100) String cargoType,
            String observations,
            MissionStatus status) {
    }

    public record UpdateMissionRequest(
            @Size(max = 30) String reference,
            UUID clientId,
            UUID driverId,
            UUID vehicleId,
            @Size(max = 255) String departureLocation,
            @Size(max = 255) String destination,
            Instant scheduledAt,
            Instant startedAt,
            Instant completedAt,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
            @Size(max = 100) String cargoType,
            String observations,
            MissionStatus status) {
    }

    public record StatusRequest(@NotNull MissionStatus status) {
    }

    public record MissionResponse(
            UUID id,
            String reference,
            UUID clientId,
            String clientName,
            UUID driverId,
            String driverFirstName,
            String driverLastName,
            UUID vehicleId,
            String vehicleRegistrationNumber,
            UUID createdById,
            String departureLocation,
            String destination,
            Instant scheduledAt,
            Instant startedAt,
            Instant completedAt,
            BigDecimal weightKg,
            String cargoType,
            String observations,
            MissionStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }
}

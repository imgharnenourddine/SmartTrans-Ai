package com.smarttrans.ai.driver;

import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.Forbidden;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Driver;
import com.smarttrans.ai.domain.DriverStatus;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class DriverService {

    private static final int LICENSE_EXPIRING_SOON_DAYS = 30;
    private static final Set<DriverStatus> AVAILABILITY_STATUSES = Set.of(
            DriverStatus.AVAILABLE, DriverStatus.UNAVAILABLE);

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Inject
    private CurrentUser currentUser;

    public DriverResponse create(CreateDriverRequest request) {
        String licenseNumber = request.licenseNumber().trim();
        if (existsByLicense(licenseNumber)) {
            throw new Conflict("License number already in use: " + licenseNumber);
        }
        UserAccount user = resolveUser(request.userId());
        if (user != null && existsByUser(user.getId())) {
            throw new Conflict("User already linked to a driver: " + user.getId());
        }
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFirstName(request.firstName().trim());
        driver.setLastName(request.lastName().trim());
        driver.setPhone(request.phone().trim());
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiresAt(request.licenseExpiresAt());
        driver.setExperienceYears(request.experienceYears());
        driver.setStatus(request.status() != null ? request.status() : DriverStatus.AVAILABLE);
        entityManager.persist(driver);
        entityManager.flush();
        return toResponse(driver);
    }

    public DriverResponse getById(UUID id) {
        Driver driver = find(id);
        assertCanRead(driver);
        return toResponse(driver);
    }

    public DriverResponse getMe() {
        List<Driver> drivers = entityManager.createQuery(
                        "select d from Driver d left join fetch d.user where d.user.id = :userId", Driver.class)
                .setParameter("userId", currentUser.id())
                .getResultList();
        if (drivers.isEmpty()) {
            throw new NotFound("Driver profile not found for current user");
        }
        return toResponse(drivers.getFirst());
    }

    public List<DriverResponse> search(String search, DriverStatus status, Boolean licenseExpired, Boolean availableOnly) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return entityManager.createQuery("""
                        SELECT d FROM Driver d
                        WHERE (:search IS NULL OR :search = '' OR
                               LOWER(d.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                               LOWER(d.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                               LOWER(d.phone) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                               LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                          AND (:status IS NULL OR d.status = :status)
                          AND (:licenseExpired IS NULL OR
                               (:licenseExpired = TRUE AND d.licenseExpiresAt < :today) OR
                               (:licenseExpired = FALSE AND d.licenseExpiresAt >= :today))
                          AND (:availableOnly IS NULL OR :availableOnly = FALSE OR
                               (d.status = com.smarttrans.ai.domain.DriverStatus.AVAILABLE
                                AND d.licenseExpiresAt >= :today))
                        ORDER BY d.lastName ASC, d.firstName ASC
                        """, Driver.class)
                .setParameter("search", normalizeSearch(search))
                .setParameter("status", status)
                .setParameter("licenseExpired", licenseExpired)
                .setParameter("availableOnly", availableOnly)
                .setParameter("today", today)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DriverResponse> findExpiringLicenses(int withinDays) {
        if (withinDays < 0) {
            throw new Business("withinDays must be zero or positive");
        }
        LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(withinDays);
        return entityManager.createQuery("""
                        SELECT d FROM Driver d
                        WHERE d.licenseExpiresAt <= :deadline
                          AND d.status <> com.smarttrans.ai.domain.DriverStatus.SUSPENDED
                        ORDER BY d.licenseExpiresAt ASC, d.lastName ASC, d.firstName ASC
                        """, Driver.class)
                .setParameter("deadline", deadline)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DriverResponse update(UUID id, UpdateDriverRequest request) {
        Driver driver = find(id);
        if (request.licenseNumber() != null) {
            String licenseNumber = request.licenseNumber().trim();
            if (!licenseNumber.equalsIgnoreCase(driver.getLicenseNumber()) && existsByLicense(licenseNumber)) {
                throw new Conflict("License number already in use: " + licenseNumber);
            }
            driver.setLicenseNumber(licenseNumber);
        }
        if (request.userId() != null) {
            UserAccount user = resolveUser(request.userId());
            UUID currentUserId = driver.getUser() != null ? driver.getUser().getId() : null;
            if (user != null && !user.getId().equals(currentUserId) && existsByUser(user.getId())) {
                throw new Conflict("User already linked to a driver: " + user.getId());
            }
            driver.setUser(user);
        }
        if (request.status() == DriverStatus.SUSPENDED && driver.getStatus() == DriverStatus.ON_MISSION) {
            throw new Conflict("Cannot suspend a driver currently on mission");
        }
        if (request.firstName() != null) {
            driver.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            driver.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            driver.setPhone(request.phone().trim());
        }
        if (request.licenseExpiresAt() != null) {
            driver.setLicenseExpiresAt(request.licenseExpiresAt());
        }
        if (request.experienceYears() != null) {
            driver.setExperienceYears(request.experienceYears());
        }
        if (request.status() != null) {
            driver.setStatus(request.status());
        }
        return toResponse(driver);
    }

    public DriverResponse updateAvailability(UUID id, UpdateDriverAvailabilityRequest request) {
        Driver driver = find(id);
        assertCanManageAvailability(driver);
        DriverStatus targetStatus = request.status();
        if (!AVAILABILITY_STATUSES.contains(targetStatus)) {
            throw new Business("Availability can only be set to AVAILABLE or UNAVAILABLE");
        }
        if (driver.getStatus() == DriverStatus.ON_MISSION) {
            throw new Conflict("Cannot change availability while driver is on mission");
        }
        if (driver.getStatus() == DriverStatus.SUSPENDED) {
            throw new Conflict("Suspended driver availability cannot be changed from this endpoint");
        }
        if (targetStatus == DriverStatus.AVAILABLE && isLicenseExpired(driver)) {
            throw new Business("Cannot set driver as available: driving license is expired");
        }
        driver.setStatus(targetStatus);
        return toResponse(driver);
    }

    public DriverResponse deactivate(UUID id) {
        Driver driver = find(id);
        if (driver.getStatus() == DriverStatus.ON_MISSION) {
            throw new Conflict("Cannot deactivate a driver currently on mission");
        }
        driver.setStatus(DriverStatus.SUSPENDED);
        return toResponse(driver);
    }

    public void delete(UUID id) {
        deactivate(id);
    }

    private Driver find(UUID id) {
        Driver driver = entityManager.find(Driver.class, id);
        if (driver == null) {
            throw new NotFound("Driver not found with id: " + id);
        }
        return driver;
    }

    private UserAccount resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        UserAccount user = entityManager.find(UserAccount.class, userId);
        if (user == null) {
            throw new Business("User not found with id: " + userId);
        }
        return user;
    }

    private boolean existsByLicense(String licenseNumber) {
        Long count = entityManager.createQuery(
                        "select count(d) from Driver d where d.licenseNumber = :license", Long.class)
                .setParameter("license", licenseNumber)
                .getSingleResult();
        return count > 0;
    }

    private boolean existsByUser(UUID userId) {
        Long count = entityManager.createQuery(
                        "select count(d) from Driver d where d.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    private void assertCanRead(Driver driver) {
        if (currentUser.hasAny("ADMIN", "TRANSPORT_MANAGER")) {
            return;
        }
        if (currentUser.hasRole("DRIVER") && isLinkedToCurrentUser(driver)) {
            return;
        }
        throw new Forbidden();
    }

    private void assertCanManageAvailability(Driver driver) {
        if (currentUser.hasAny("ADMIN", "TRANSPORT_MANAGER")) {
            return;
        }
        if (currentUser.hasRole("DRIVER") && isLinkedToCurrentUser(driver)) {
            return;
        }
        throw new Forbidden();
    }

    private boolean isLinkedToCurrentUser(Driver driver) {
        return driver.getUser() != null && driver.getUser().getId().equals(currentUser.id());
    }

    private boolean isLicenseExpired(Driver driver) {
        return driver.getLicenseExpiresAt().isBefore(LocalDate.now(ZoneOffset.UTC));
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DriverResponse toResponse(Driver driver) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, driver.getLicenseExpiresAt());
        boolean expired = daysUntilExpiry < 0;
        return new DriverResponse(
                driver.getId(),
                driver.getUser() != null ? driver.getUser().getId() : null,
                driver.getFirstName(),
                driver.getLastName(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiresAt(),
                driver.getExperienceYears(),
                driver.getStatus(),
                expired,
                !expired && daysUntilExpiry <= LICENSE_EXPIRING_SOON_DAYS,
                daysUntilExpiry,
                driver.getStatus() == DriverStatus.AVAILABLE && !expired,
                driver.getCreatedAt(),
                driver.getUpdatedAt());
    }

    public record CreateDriverRequest(
            UUID userId,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Size(max = 50) String licenseNumber,
            @NotNull LocalDate licenseExpiresAt,
            @PositiveOrZero Short experienceYears,
            DriverStatus status) {
    }

    public record UpdateDriverRequest(
            UUID userId,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            @Size(max = 50) String licenseNumber,
            LocalDate licenseExpiresAt,
            @PositiveOrZero Short experienceYears,
            DriverStatus status) {
    }

    public record UpdateDriverAvailabilityRequest(@NotNull DriverStatus status) {
    }

    public record DriverResponse(
            UUID id,
            UUID userId,
            String firstName,
            String lastName,
            String phone,
            String licenseNumber,
            LocalDate licenseExpiresAt,
            Short experienceYears,
            DriverStatus status,
            boolean licenseExpired,
            boolean licenseExpiringSoon,
            long daysUntilLicenseExpiry,
            boolean availableForMission,
            Instant createdAt,
            Instant updatedAt) {
    }
}

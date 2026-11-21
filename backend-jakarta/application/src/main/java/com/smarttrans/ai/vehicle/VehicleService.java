package com.smarttrans.ai.vehicle;

import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Vehicle;
import com.smarttrans.ai.domain.VehicleStatus;
import com.smarttrans.ai.domain.VehicleType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class VehicleService {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    public VehicleResponse create(CreateVehicleRequest request) {
        String registration = request.registrationNumber().trim().toUpperCase();
        if (existsByRegistration(registration)) {
            throw new Conflict("Registration number already in use: " + registration);
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(registration);
        vehicle.setType(request.type());
        vehicle.setCapacityKg(request.capacityKg());
        vehicle.setMileageKm(request.mileageKm() != null ? request.mileageKm() : 0);
        vehicle.setConsumptionLPer100km(request.consumptionLPer100km());
        vehicle.setInsuranceExpiresAt(request.insuranceExpiresAt());
        vehicle.setTechnicalVisitExpiresAt(request.technicalVisitExpiresAt());
        vehicle.setStatus(request.status() != null ? request.status() : VehicleStatus.AVAILABLE);
        entityManager.persist(vehicle);
        entityManager.flush();
        return toResponse(vehicle);
    }

    public VehicleResponse getById(UUID id) {
        return toResponse(find(id));
    }

    public List<VehicleResponse> search(
            String search,
            VehicleStatus status,
            Boolean availableOnly,
            Integer insuranceExpiringWithinDays,
            Integer technicalVisitExpiringWithinDays) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate insuranceDeadline = deadline(insuranceExpiringWithinDays, today);
        LocalDate technicalDeadline = deadline(technicalVisitExpiringWithinDays, today);
        return entityManager.createQuery("""
                        SELECT v FROM Vehicle v
                        WHERE (:search IS NULL OR :search = '' OR
                               LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                          AND (:status IS NULL OR v.status = :status)
                          AND (:availableOnly IS NULL OR :availableOnly = FALSE OR
                               (v.status = com.smarttrans.ai.domain.VehicleStatus.AVAILABLE
                                AND (v.insuranceExpiresAt IS NULL OR v.insuranceExpiresAt >= :today)
                                AND (v.technicalVisitExpiresAt IS NULL OR v.technicalVisitExpiresAt >= :today)))
                          AND (CAST(:insuranceDeadline AS LocalDate) IS NULL OR
                               (v.insuranceExpiresAt IS NOT NULL AND v.insuranceExpiresAt <= :insuranceDeadline))
                          AND (CAST(:technicalDeadline AS LocalDate) IS NULL OR
                               (v.technicalVisitExpiresAt IS NOT NULL AND v.technicalVisitExpiresAt <= :technicalDeadline))
                        ORDER BY v.registrationNumber ASC
                        """, Vehicle.class)
                .setParameter("search", normalizeSearch(search))
                .setParameter("status", status)
                .setParameter("availableOnly", availableOnly)
                .setParameter("today", today)
                .setParameter("insuranceDeadline", insuranceDeadline)
                .setParameter("technicalDeadline", technicalDeadline)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<VehicleResponse> findWithExpiringDocuments(int withinDays) {
        if (withinDays < 0) {
            throw new Business("withinDays must be zero or positive");
        }
        LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(withinDays);
        return entityManager.createQuery("""
                        SELECT v FROM Vehicle v
                        WHERE v.status <> com.smarttrans.ai.domain.VehicleStatus.OUT_OF_SERVICE
                          AND ((v.insuranceExpiresAt IS NOT NULL AND v.insuranceExpiresAt <= :deadline)
                            OR (v.technicalVisitExpiresAt IS NOT NULL AND v.technicalVisitExpiresAt <= :deadline))
                        ORDER BY
                            CASE WHEN v.insuranceExpiresAt IS NULL THEN 1 ELSE 0 END,
                            v.insuranceExpiresAt ASC,
                            CASE WHEN v.technicalVisitExpiresAt IS NULL THEN 1 ELSE 0 END,
                            v.technicalVisitExpiresAt ASC,
                            v.registrationNumber ASC
                        """, Vehicle.class)
                .setParameter("deadline", deadline)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VehicleResponse update(UUID id, UpdateVehicleRequest request) {
        Vehicle vehicle = find(id);
        if (request.registrationNumber() != null) {
            String registration = request.registrationNumber().trim().toUpperCase();
            if (!registration.equalsIgnoreCase(vehicle.getRegistrationNumber()) && existsByRegistration(registration)) {
                throw new Conflict("Registration number already in use: " + registration);
            }
            vehicle.setRegistrationNumber(registration);
        }
        if (request.status() == VehicleStatus.OUT_OF_SERVICE && vehicle.getStatus() == VehicleStatus.IN_USE) {
            throw new Conflict("Cannot deactivate a vehicle currently in use on a mission");
        }
        if (request.type() != null) {
            vehicle.setType(request.type());
        }
        if (request.capacityKg() != null) {
            vehicle.setCapacityKg(request.capacityKg());
        }
        if (request.mileageKm() != null) {
            vehicle.setMileageKm(request.mileageKm());
        }
        if (request.consumptionLPer100km() != null) {
            vehicle.setConsumptionLPer100km(request.consumptionLPer100km());
        }
        if (request.insuranceExpiresAt() != null) {
            vehicle.setInsuranceExpiresAt(request.insuranceExpiresAt());
        }
        if (request.technicalVisitExpiresAt() != null) {
            vehicle.setTechnicalVisitExpiresAt(request.technicalVisitExpiresAt());
        }
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
        return toResponse(vehicle);
    }

    public VehicleResponse deactivate(UUID id) {
        Vehicle vehicle = find(id);
        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            throw new Conflict("Cannot deactivate a vehicle currently in use on a mission");
        }
        vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
        return toResponse(vehicle);
    }

    public void delete(UUID id) {
        deactivate(id);
    }

    private Vehicle find(UUID id) {
        Vehicle vehicle = entityManager.find(Vehicle.class, id);
        if (vehicle == null) {
            throw new NotFound("Vehicle not found with id: " + id);
        }
        return vehicle;
    }

    private boolean existsByRegistration(String registration) {
        Long count = entityManager.createQuery(
                        "select count(v) from Vehicle v where v.registrationNumber = :registration", Long.class)
                .setParameter("registration", registration)
                .getSingleResult();
        return count > 0;
    }

    private LocalDate deadline(Integer withinDays, LocalDate today) {
        if (withinDays == null) {
            return null;
        }
        if (withinDays < 0) {
            throw new Business("Expiring window (days) must be zero or positive");
        }
        return today.plusDays(withinDays);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getType(),
                vehicle.getCapacityKg(),
                vehicle.getMileageKm(),
                vehicle.getConsumptionLPer100km(),
                vehicle.getInsuranceExpiresAt(),
                vehicle.getTechnicalVisitExpiresAt(),
                vehicle.getStatus(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }

    public record CreateVehicleRequest(
            @NotBlank @Size(max = 20) String registrationNumber,
            @NotNull VehicleType type,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal capacityKg,
            @PositiveOrZero Integer mileageKm,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal consumptionLPer100km,
            LocalDate insuranceExpiresAt,
            LocalDate technicalVisitExpiresAt,
            VehicleStatus status) {
    }

    public record UpdateVehicleRequest(
            @Size(max = 20) String registrationNumber,
            VehicleType type,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal capacityKg,
            @PositiveOrZero Integer mileageKm,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal consumptionLPer100km,
            LocalDate insuranceExpiresAt,
            LocalDate technicalVisitExpiresAt,
            VehicleStatus status) {
    }

    public record VehicleResponse(
            UUID id,
            String registrationNumber,
            VehicleType type,
            BigDecimal capacityKg,
            Integer mileageKm,
            BigDecimal consumptionLPer100km,
            LocalDate insuranceExpiresAt,
            LocalDate technicalVisitExpiresAt,
            VehicleStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }
}

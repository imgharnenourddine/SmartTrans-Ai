package com.smarttrans.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleType type;

    @Column(name = "capacity_kg", precision = 12, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "mileage_km", nullable = false)
    private Integer mileageKm = 0;

    @Column(name = "consumption_l_per_100km", precision = 6, scale = 2)
    private BigDecimal consumptionLPer100km;

    @Column(name = "insurance_expires_at")
    private LocalDate insuranceExpiresAt;

    @Column(name = "technical_visit_expires_at")
    private LocalDate technicalVisitExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleStatus status;

    public UUID getId() {
        return id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public BigDecimal getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(BigDecimal capacityKg) {
        this.capacityKg = capacityKg;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    public void setMileageKm(Integer mileageKm) {
        this.mileageKm = mileageKm;
    }

    public BigDecimal getConsumptionLPer100km() {
        return consumptionLPer100km;
    }

    public void setConsumptionLPer100km(BigDecimal consumptionLPer100km) {
        this.consumptionLPer100km = consumptionLPer100km;
    }

    public LocalDate getInsuranceExpiresAt() {
        return insuranceExpiresAt;
    }

    public void setInsuranceExpiresAt(LocalDate insuranceExpiresAt) {
        this.insuranceExpiresAt = insuranceExpiresAt;
    }

    public LocalDate getTechnicalVisitExpiresAt() {
        return technicalVisitExpiresAt;
    }

    public void setTechnicalVisitExpiresAt(LocalDate technicalVisitExpiresAt) {
        this.technicalVisitExpiresAt = technicalVisitExpiresAt;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
}

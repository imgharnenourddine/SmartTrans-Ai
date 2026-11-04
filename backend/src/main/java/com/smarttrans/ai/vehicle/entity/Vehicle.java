package com.smarttrans.ai.vehicle.entity;

import com.smarttrans.ai.common.entity.AuditableEntity;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.enums.VehicleType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
	@Builder.Default
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
}

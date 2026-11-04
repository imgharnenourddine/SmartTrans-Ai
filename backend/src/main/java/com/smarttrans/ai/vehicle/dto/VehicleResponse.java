package com.smarttrans.ai.vehicle.dto;

import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.enums.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

	private UUID id;
	private String registrationNumber;
	private VehicleType type;
	private BigDecimal capacityKg;
	private Integer mileageKm;
	private BigDecimal consumptionLPer100km;
	private LocalDate insuranceExpiresAt;
	private LocalDate technicalVisitExpiresAt;
	private VehicleStatus status;
	private Instant createdAt;
	private Instant updatedAt;
}

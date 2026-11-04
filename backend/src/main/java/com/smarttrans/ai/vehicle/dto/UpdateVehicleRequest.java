package com.smarttrans.ai.vehicle.dto;

import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class UpdateVehicleRequest {

	@Size(max = 20)
	private String registrationNumber;

	private VehicleType type;

	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal capacityKg;

	@PositiveOrZero
	private Integer mileageKm;

	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal consumptionLPer100km;

	private LocalDate insuranceExpiresAt;

	private LocalDate technicalVisitExpiresAt;

	private VehicleStatus status;
}

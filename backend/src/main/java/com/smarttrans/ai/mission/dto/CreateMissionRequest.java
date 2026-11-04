package com.smarttrans.ai.mission.dto;

import com.smarttrans.ai.mission.enums.MissionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
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
public class CreateMissionRequest {

	@NotBlank
	@Size(max = 30)
	private String reference;

	@NotNull
	private UUID clientId;

	private UUID driverId;

	private UUID vehicleId;

	private UUID createdById;

	@NotBlank
	@Size(max = 255)
	private String departureLocation;

	@NotBlank
	@Size(max = 255)
	private String destination;

	@NotNull
	private Instant scheduledAt;

	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal weightKg;

	@Size(max = 100)
	private String cargoType;

	private String observations;

	private MissionStatus status;
}

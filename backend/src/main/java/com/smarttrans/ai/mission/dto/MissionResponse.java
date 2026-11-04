package com.smarttrans.ai.mission.dto;

import com.smarttrans.ai.mission.enums.MissionStatus;
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
public class MissionResponse {

	private UUID id;
	private String reference;
	private UUID clientId;
	private String clientName;
	private UUID driverId;
	private String driverFirstName;
	private String driverLastName;
	private UUID vehicleId;
	private String vehicleRegistrationNumber;
	private UUID createdById;
	private String departureLocation;
	private String destination;
	private Instant scheduledAt;
	private Instant startedAt;
	private Instant completedAt;
	private BigDecimal weightKg;
	private String cargoType;
	private String observations;
	private MissionStatus status;
	private Instant createdAt;
	private Instant updatedAt;
}

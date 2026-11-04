package com.smarttrans.ai.driver.dto;

import com.smarttrans.ai.driver.enums.DriverStatus;
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
public class DriverResponse {

	private UUID id;
	private UUID userId;
	private String firstName;
	private String lastName;
	private String phone;
	private String licenseNumber;
	private LocalDate licenseExpiresAt;
	private Short experienceYears;
	private DriverStatus status;
	private boolean licenseExpired;
	private boolean licenseExpiringSoon;
	private long daysUntilLicenseExpiry;
	private boolean availableForMission;
	private Instant createdAt;
	private Instant updatedAt;
}

package com.smarttrans.ai.driver.mapper;

import com.smarttrans.ai.driver.dto.CreateDriverRequest;
import com.smarttrans.ai.driver.dto.DriverResponse;
import com.smarttrans.ai.driver.dto.UpdateDriverRequest;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.user.entity.User;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

	private static final int LICENSE_EXPIRING_SOON_DAYS = 30;

	public Driver toEntity(CreateDriverRequest request, User user) {
		return Driver.builder()
				.user(user)
				.firstName(request.getFirstName().trim())
				.lastName(request.getLastName().trim())
				.phone(request.getPhone().trim())
				.licenseNumber(request.getLicenseNumber().trim())
				.licenseExpiresAt(request.getLicenseExpiresAt())
				.experienceYears(request.getExperienceYears())
				.status(request.getStatus() != null ? request.getStatus() : DriverStatus.AVAILABLE)
				.build();
	}

	public void updateEntity(Driver driver, UpdateDriverRequest request, User user, boolean updateUser) {
		if (updateUser) {
			driver.setUser(user);
		}
		if (request.getFirstName() != null) {
			driver.setFirstName(request.getFirstName().trim());
		}
		if (request.getLastName() != null) {
			driver.setLastName(request.getLastName().trim());
		}
		if (request.getPhone() != null) {
			driver.setPhone(request.getPhone().trim());
		}
		if (request.getLicenseNumber() != null) {
			driver.setLicenseNumber(request.getLicenseNumber().trim());
		}
		if (request.getLicenseExpiresAt() != null) {
			driver.setLicenseExpiresAt(request.getLicenseExpiresAt());
		}
		if (request.getExperienceYears() != null) {
			driver.setExperienceYears(request.getExperienceYears());
		}
		if (request.getStatus() != null) {
			driver.setStatus(request.getStatus());
		}
	}

	public DriverResponse toResponse(Driver driver) {
		DriverResponse response = DriverResponse.builder()
				.id(driver.getId())
				.userId(driver.getUser() != null ? driver.getUser().getId() : null)
				.firstName(driver.getFirstName())
				.lastName(driver.getLastName())
				.phone(driver.getPhone())
				.licenseNumber(driver.getLicenseNumber())
				.licenseExpiresAt(driver.getLicenseExpiresAt())
				.experienceYears(driver.getExperienceYears())
				.status(driver.getStatus())
				.createdAt(driver.getCreatedAt())
				.updatedAt(driver.getUpdatedAt())
				.build();
		enrichLicenseInfo(response, driver);
		return response;
	}

	private void enrichLicenseInfo(DriverResponse response, Driver driver) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expiresAt = driver.getLicenseExpiresAt();
		long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiresAt);
		boolean expired = daysUntilExpiry < 0;

		response.setLicenseExpired(expired);
		response.setLicenseExpiringSoon(!expired && daysUntilExpiry <= LICENSE_EXPIRING_SOON_DAYS);
		response.setDaysUntilLicenseExpiry(daysUntilExpiry);
		response.setAvailableForMission(driver.getStatus() == DriverStatus.AVAILABLE && !expired);
	}
}

package com.smarttrans.ai.driver.service;

import com.smarttrans.ai.driver.dto.CreateDriverRequest;
import com.smarttrans.ai.driver.dto.DriverResponse;
import com.smarttrans.ai.driver.dto.UpdateDriverAvailabilityRequest;
import com.smarttrans.ai.driver.dto.UpdateDriverRequest;
import com.smarttrans.ai.driver.enums.DriverStatus;
import java.util.List;
import java.util.UUID;

public interface DriverService {

	DriverResponse create(CreateDriverRequest request);

	DriverResponse getById(UUID id);

	DriverResponse getMe();

	List<DriverResponse> search(String search, DriverStatus status, Boolean licenseExpired, Boolean availableOnly);

	List<DriverResponse> findExpiringLicenses(int withinDays);

	DriverResponse update(UUID id, UpdateDriverRequest request);

	DriverResponse updateAvailability(UUID id, UpdateDriverAvailabilityRequest request);

	DriverResponse deactivate(UUID id);

	void delete(UUID id);
}

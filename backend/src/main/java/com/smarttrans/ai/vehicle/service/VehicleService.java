package com.smarttrans.ai.vehicle.service;

import com.smarttrans.ai.vehicle.dto.CreateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.UpdateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.VehicleResponse;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import java.util.List;
import java.util.UUID;

public interface VehicleService {

	VehicleResponse create(CreateVehicleRequest request);

	VehicleResponse getById(UUID id);

	List<VehicleResponse> search(String search, VehicleStatus status, Boolean availableOnly,
			Integer insuranceExpiringWithinDays, Integer technicalVisitExpiringWithinDays);

	List<VehicleResponse> findWithExpiringDocuments(int withinDays);

	VehicleResponse update(UUID id, UpdateVehicleRequest request);

	VehicleResponse deactivate(UUID id);

	void delete(UUID id);
}

package com.smarttrans.ai.vehicle.service.impl;

import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.exception.BusinessException;
import com.smarttrans.ai.exception.ConflictException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.vehicle.dto.CreateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.UpdateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.VehicleResponse;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.mapper.VehicleMapper;
import com.smarttrans.ai.vehicle.repository.VehicleRepository;
import com.smarttrans.ai.vehicle.service.VehicleService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

	private final VehicleRepository vehicleRepository;
	private final VehicleMapper vehicleMapper;
	private final ActivityService activityService;

	@Override
	public VehicleResponse create(CreateVehicleRequest request) {
		String registration = request.getRegistrationNumber().trim().toUpperCase();
		if (vehicleRepository.existsByRegistrationNumber(registration)) {
			throw new ConflictException("Registration number already in use: " + registration);
		}
		Vehicle saved = vehicleRepository.save(vehicleMapper.toEntity(request));
		activityService.record(
				ActivityAction.CREATE,
				ActivityEntityType.VEHICLE,
				saved.getId(),
				ActivityEntityType.metadata("registrationNumber", saved.getRegistrationNumber()));
		return vehicleMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public VehicleResponse getById(UUID id) {
		return vehicleMapper.toResponse(findVehicle(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<VehicleResponse> search(String search, VehicleStatus status, Boolean availableOnly,
			Integer insuranceExpiringWithinDays, Integer technicalVisitExpiringWithinDays) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate insuranceDeadline = resolveDeadline(insuranceExpiringWithinDays, today);
		LocalDate technicalVisitDeadline = resolveDeadline(technicalVisitExpiringWithinDays, today);
		return vehicleRepository
				.search(normalizeSearch(search), status, availableOnly, insuranceDeadline, technicalVisitDeadline, today)
				.stream()
				.map(vehicleMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<VehicleResponse> findWithExpiringDocuments(int withinDays) {
		if (withinDays < 0) {
			throw new BusinessException("withinDays must be zero or positive");
		}
		LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(withinDays);
		return vehicleRepository.findWithExpiringDocuments(deadline).stream()
				.map(vehicleMapper::toResponse)
				.toList();
	}

	@Override
	public VehicleResponse update(UUID id, UpdateVehicleRequest request) {
		Vehicle vehicle = findVehicle(id);
		if (request.getRegistrationNumber() != null) {
			String registration = request.getRegistrationNumber().trim().toUpperCase();
			if (!registration.equalsIgnoreCase(vehicle.getRegistrationNumber())
					&& vehicleRepository.existsByRegistrationNumber(registration)) {
				throw new ConflictException("Registration number already in use: " + registration);
			}
		}
		if (request.getStatus() == VehicleStatus.OUT_OF_SERVICE && vehicle.getStatus() == VehicleStatus.IN_USE) {
			throw new ConflictException("Cannot deactivate a vehicle currently in use on a mission");
		}
		VehicleStatus previousStatus = vehicle.getStatus();
		vehicleMapper.updateEntity(vehicle, request);
		recordVehicleChange(vehicle, previousStatus);
		return vehicleMapper.toResponse(vehicle);
	}

	@Override
	public VehicleResponse deactivate(UUID id) {
		Vehicle vehicle = findVehicle(id);
		if (vehicle.getStatus() == VehicleStatus.IN_USE) {
			throw new ConflictException("Cannot deactivate a vehicle currently in use on a mission");
		}
		VehicleStatus previousStatus = vehicle.getStatus();
		vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
		recordVehicleChange(vehicle, previousStatus);
		return vehicleMapper.toResponse(vehicle);
	}

	@Override
	public void delete(UUID id) {
		deactivate(id);
	}

	private void recordVehicleChange(Vehicle vehicle, VehicleStatus previousStatus) {
		if (vehicle.getStatus() != previousStatus) {
			activityService.record(
					ActivityAction.STATUS_CHANGE,
					ActivityEntityType.VEHICLE,
					vehicle.getId(),
					ActivityEntityType.metadata(
							"registrationNumber", vehicle.getRegistrationNumber(),
							"from", previousStatus.name(),
							"to", vehicle.getStatus().name()));
			return;
		}
		activityService.record(
				ActivityAction.UPDATE,
				ActivityEntityType.VEHICLE,
				vehicle.getId(),
				ActivityEntityType.metadata("registrationNumber", vehicle.getRegistrationNumber()));
	}

	private Vehicle findVehicle(UUID id) {
		return vehicleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
	}

	private LocalDate resolveDeadline(Integer withinDays, LocalDate today) {
		if (withinDays == null) {
			return null;
		}
		if (withinDays < 0) {
			throw new BusinessException("Expiring window (days) must be zero or positive");
		}
		return today.plusDays(withinDays);
	}

	private String normalizeSearch(String search) {
		if (search == null) {
			return null;
		}
		String trimmed = search.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

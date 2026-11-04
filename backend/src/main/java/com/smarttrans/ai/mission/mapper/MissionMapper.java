package com.smarttrans.ai.mission.mapper;

import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.mission.dto.CreateMissionRequest;
import com.smarttrans.ai.mission.dto.MissionResponse;
import com.smarttrans.ai.mission.dto.UpdateMissionRequest;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.mission.enums.MissionStatus;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class MissionMapper {

	public Mission toEntity(
			CreateMissionRequest request,
			Client client,
			Driver driver,
			Vehicle vehicle,
			User createdBy) {
		return Mission.builder()
				.reference(request.getReference().trim().toUpperCase())
				.client(client)
				.driver(driver)
				.vehicle(vehicle)
				.createdBy(createdBy)
				.departureLocation(request.getDepartureLocation().trim())
				.destination(request.getDestination().trim())
				.scheduledAt(request.getScheduledAt())
				.weightKg(request.getWeightKg())
				.cargoType(trimToNull(request.getCargoType()))
				.observations(trimToNull(request.getObservations()))
				.status(request.getStatus() != null ? request.getStatus() : MissionStatus.PLANIFIED)
				.build();
	}

	public void updateEntity(
			Mission mission,
			UpdateMissionRequest request,
			Client client,
			boolean updateClient,
			Driver driver,
			boolean updateDriver,
			Vehicle vehicle,
			boolean updateVehicle) {
		if (request.getReference() != null) {
			mission.setReference(request.getReference().trim().toUpperCase());
		}
		if (updateClient) {
			mission.setClient(client);
		}
		if (updateDriver) {
			mission.setDriver(driver);
		}
		if (updateVehicle) {
			mission.setVehicle(vehicle);
		}
		if (request.getDepartureLocation() != null) {
			mission.setDepartureLocation(request.getDepartureLocation().trim());
		}
		if (request.getDestination() != null) {
			mission.setDestination(request.getDestination().trim());
		}
		if (request.getScheduledAt() != null) {
			mission.setScheduledAt(request.getScheduledAt());
		}
		if (request.getStartedAt() != null) {
			mission.setStartedAt(request.getStartedAt());
		}
		if (request.getCompletedAt() != null) {
			mission.setCompletedAt(request.getCompletedAt());
		}
		if (request.getWeightKg() != null) {
			mission.setWeightKg(request.getWeightKg());
		}
		if (request.getCargoType() != null) {
			mission.setCargoType(trimToNull(request.getCargoType()));
		}
		if (request.getObservations() != null) {
			mission.setObservations(trimToNull(request.getObservations()));
		}
	}

	public MissionResponse toResponse(Mission mission) {
		return MissionResponse.builder()
				.id(mission.getId())
				.reference(mission.getReference())
				.clientId(mission.getClient() != null ? mission.getClient().getId() : null)
				.clientName(mission.getClient() != null ? mission.getClient().getCompanyName() : null)
				.driverId(mission.getDriver() != null ? mission.getDriver().getId() : null)
				.driverFirstName(mission.getDriver() != null ? mission.getDriver().getFirstName() : null)
				.driverLastName(mission.getDriver() != null ? mission.getDriver().getLastName() : null)
				.vehicleId(mission.getVehicle() != null ? mission.getVehicle().getId() : null)
				.vehicleRegistrationNumber(
						mission.getVehicle() != null ? mission.getVehicle().getRegistrationNumber() : null)
				.createdById(mission.getCreatedBy() != null ? mission.getCreatedBy().getId() : null)
				.departureLocation(mission.getDepartureLocation())
				.destination(mission.getDestination())
				.scheduledAt(mission.getScheduledAt())
				.startedAt(mission.getStartedAt())
				.completedAt(mission.getCompletedAt())
				.weightKg(mission.getWeightKg())
				.cargoType(mission.getCargoType())
				.observations(mission.getObservations())
				.status(mission.getStatus())
				.createdAt(mission.getCreatedAt())
				.updatedAt(mission.getUpdatedAt())
				.build();
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

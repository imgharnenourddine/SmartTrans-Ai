package com.smarttrans.ai.vehicle.mapper;

import com.smarttrans.ai.vehicle.dto.CreateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.UpdateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.VehicleResponse;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

	public Vehicle toEntity(CreateVehicleRequest request) {
		return Vehicle.builder()
				.registrationNumber(request.getRegistrationNumber().trim().toUpperCase())
				.type(request.getType())
				.capacityKg(request.getCapacityKg())
				.mileageKm(request.getMileageKm() != null ? request.getMileageKm() : 0)
				.consumptionLPer100km(request.getConsumptionLPer100km())
				.insuranceExpiresAt(request.getInsuranceExpiresAt())
				.technicalVisitExpiresAt(request.getTechnicalVisitExpiresAt())
				.status(request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE)
				.build();
	}

	public void updateEntity(Vehicle vehicle, UpdateVehicleRequest request) {
		if (request.getRegistrationNumber() != null) {
			vehicle.setRegistrationNumber(request.getRegistrationNumber().trim().toUpperCase());
		}
		if (request.getType() != null) {
			vehicle.setType(request.getType());
		}
		if (request.getCapacityKg() != null) {
			vehicle.setCapacityKg(request.getCapacityKg());
		}
		if (request.getMileageKm() != null) {
			vehicle.setMileageKm(request.getMileageKm());
		}
		if (request.getConsumptionLPer100km() != null) {
			vehicle.setConsumptionLPer100km(request.getConsumptionLPer100km());
		}
		if (request.getInsuranceExpiresAt() != null) {
			vehicle.setInsuranceExpiresAt(request.getInsuranceExpiresAt());
		}
		if (request.getTechnicalVisitExpiresAt() != null) {
			vehicle.setTechnicalVisitExpiresAt(request.getTechnicalVisitExpiresAt());
		}
		if (request.getStatus() != null) {
			vehicle.setStatus(request.getStatus());
		}
	}

	public VehicleResponse toResponse(Vehicle vehicle) {
		return VehicleResponse.builder()
				.id(vehicle.getId())
				.registrationNumber(vehicle.getRegistrationNumber())
				.type(vehicle.getType())
				.capacityKg(vehicle.getCapacityKg())
				.mileageKm(vehicle.getMileageKm())
				.consumptionLPer100km(vehicle.getConsumptionLPer100km())
				.insuranceExpiresAt(vehicle.getInsuranceExpiresAt())
				.technicalVisitExpiresAt(vehicle.getTechnicalVisitExpiresAt())
				.status(vehicle.getStatus())
				.createdAt(vehicle.getCreatedAt())
				.updatedAt(vehicle.getUpdatedAt())
				.build();
	}
}

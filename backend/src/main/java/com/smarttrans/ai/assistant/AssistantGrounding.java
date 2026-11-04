package com.smarttrans.ai.assistant;

import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import java.util.List;

public record AssistantGrounding(
		String query,
		boolean overview,
		boolean suggest,
		long missionCount,
		long planifiedCount,
		long inProgressCount,
		List<Mission> missions,
		List<Mission> unassignedMissions,
		List<Driver> drivers,
		List<Driver> expiringDrivers,
		List<Driver> availableDrivers,
		List<Vehicle> vehicles,
		List<Vehicle> expiringVehicles,
		List<Vehicle> availableVehicles,
		List<Client> clients) {
}

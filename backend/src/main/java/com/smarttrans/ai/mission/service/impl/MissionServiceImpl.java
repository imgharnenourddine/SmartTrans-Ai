package com.smarttrans.ai.mission.service.impl;

import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.client.repository.ClientRepository;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.driver.repository.DriverRepository;
import com.smarttrans.ai.exception.BusinessException;
import com.smarttrans.ai.exception.ConflictException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.mission.dto.CreateMissionRequest;
import com.smarttrans.ai.mission.dto.MissionResponse;
import com.smarttrans.ai.mission.dto.MissionStatusUpdateRequest;
import com.smarttrans.ai.mission.dto.UpdateMissionRequest;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.mission.enums.MissionStatus;
import com.smarttrans.ai.mission.mapper.MissionMapper;
import com.smarttrans.ai.mission.repository.MissionRepository;
import com.smarttrans.ai.mission.service.MissionService;
import com.smarttrans.ai.security.service.UserPrincipal;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.repository.VehicleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

	private static final Map<MissionStatus, Set<MissionStatus>> ALLOWED_TRANSITIONS = Map.of(
			MissionStatus.PLANIFIED, Set.of(MissionStatus.IN_PROGRESS, MissionStatus.CANCELLED),
			MissionStatus.IN_PROGRESS, Set.of(MissionStatus.COMPLETED, MissionStatus.CANCELLED),
			MissionStatus.COMPLETED, Set.of(),
			MissionStatus.CANCELLED, Set.of());

	private final MissionRepository missionRepository;
	private final ClientRepository clientRepository;
	private final DriverRepository driverRepository;
	private final VehicleRepository vehicleRepository;
	private final UserRepository userRepository;
	private final MissionMapper missionMapper;
	private final ActivityService activityService;

	@Override
	public MissionResponse create(CreateMissionRequest request) {
		String reference = request.getReference().trim().toUpperCase();
		if (missionRepository.existsByReference(reference)) {
			throw new ConflictException("Mission reference already in use: " + reference);
		}

		Client client = findClient(request.getClientId());
		Driver driver = resolveDriver(request.getDriverId());
		Vehicle vehicle = resolveVehicle(request.getVehicleId());
		User createdBy = resolveUser(request.getCreatedById());
		if (createdBy == null) {
			createdBy = findCurrentUserOrNull();
		}

		if (request.getStatus() != MissionStatus.PLANIFIED && request.getStatus() != null) {
			validateTransition(MissionStatus.PLANIFIED, request.getStatus());
		}

		Mission mission = missionMapper.toEntity(request, client, driver, vehicle, createdBy);
		applyStatusTimestamps(mission, mission.getStatus());
		if (mission.getStatus() == MissionStatus.IN_PROGRESS) {
			startMissionResources(mission);
		}
		Mission saved = missionRepository.save(mission);
		activityService.record(
				ActivityAction.CREATE,
				ActivityEntityType.MISSION,
				saved.getId(),
				ActivityEntityType.metadata("reference", saved.getReference(), "status", saved.getStatus().name()));
		return missionMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public MissionResponse getById(UUID id) {
		Mission mission = findMission(id);
		assertCanReadMission(mission);
		return missionMapper.toResponse(mission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MissionResponse> search(String search, MissionStatus status, UUID driverId, UUID vehicleId,
			UUID clientId, Instant scheduledFrom, Instant scheduledTo) {
		UUID effectiveDriverId = driverId;
		if (!hasAnyRole("ADMIN", "TRANSPORT_MANAGER")) {
			effectiveDriverId = requireCurrentDriverId();
		}
		String normalizedSearch = normalizeSearch(search);
		boolean unfiltered = normalizedSearch == null
				&& status == null
				&& vehicleId == null
				&& clientId == null
				&& scheduledFrom == null
				&& scheduledTo == null;
		if (unfiltered) {
			List<Mission> missions = effectiveDriverId == null
					? missionRepository.findAllByOrderByScheduledAtDescReferenceAsc()
					: missionRepository.findByDriver_IdOrderByScheduledAtDescReferenceAsc(effectiveDriverId);
			return missions.stream().map(missionMapper::toResponse).toList();
		}
		return missionRepository
				.search(normalizedSearch, status, effectiveDriverId, vehicleId, clientId,
						scheduledFrom, scheduledTo)
				.stream()
				.map(missionMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MissionResponse> getMyMissions() {
		UUID driverId = requireCurrentDriverId();
		return missionRepository.findByDriver_Id(driverId).stream()
				.map(missionMapper::toResponse)
				.toList();
	}

	@Override
	public MissionResponse update(UUID id, UpdateMissionRequest request) {
		if (!hasAnyRole("ADMIN", "TRANSPORT_MANAGER")) {
			throw new AccessDeniedException("Access denied");
		}
		Mission mission = findMission(id);

		if (request.getReference() != null) {
			String reference = request.getReference().trim().toUpperCase();
			if (!reference.equalsIgnoreCase(mission.getReference())
					&& missionRepository.existsByReference(reference)) {
				throw new ConflictException("Mission reference already in use: " + reference);
			}
		}

		boolean updateClient = request.getClientId() != null;
		Client client = updateClient ? findClient(request.getClientId()) : null;

		boolean updateDriver = request.getDriverId() != null;
		Driver driver = updateDriver ? resolveDriver(request.getDriverId()) : null;

		boolean updateVehicle = request.getVehicleId() != null;
		Vehicle vehicle = updateVehicle ? resolveVehicle(request.getVehicleId()) : null;

		assertNotStartedForReassignment(mission, updateDriver, updateVehicle, driver, vehicle);

		MissionStatus previousStatus = mission.getStatus();
		missionMapper.updateEntity(
				mission, request, client, updateClient, driver, updateDriver, vehicle, updateVehicle);

		if (request.getStatus() != null && request.getStatus() != previousStatus) {
			transitionMission(mission, request.getStatus());
		}
		recordMissionChange(mission, previousStatus);

		return missionMapper.toResponse(mission);
	}

	@Override
	public MissionResponse updateStatus(UUID id, MissionStatusUpdateRequest request) {
		Mission mission = findMission(id);
		assertCanManageMission(mission);
		if (hasRole("DRIVER") && !hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
				&& request.getStatus() == MissionStatus.CANCELLED) {
			throw new AccessDeniedException("Drivers cannot cancel missions");
		}
		MissionStatus previousStatus = mission.getStatus();
		transitionMission(mission, request.getStatus());
		recordMissionChange(mission, previousStatus);
		return missionMapper.toResponse(mission);
	}

	@Override
	public void delete(UUID id) {
		Mission mission = findMission(id);
		if (mission.getStatus() == MissionStatus.IN_PROGRESS
				|| mission.getStatus() == MissionStatus.COMPLETED) {
			throw new ConflictException(
					"Only planned or cancelled missions can be deleted: " + mission.getReference());
		}
		activityService.record(
				ActivityAction.DELETE,
				ActivityEntityType.MISSION,
				mission.getId(),
				ActivityEntityType.metadata("reference", mission.getReference()));
		missionRepository.delete(mission);
	}

	private void recordMissionChange(Mission mission, MissionStatus previousStatus) {
		if (mission.getStatus() != previousStatus) {
			activityService.record(
					ActivityAction.STATUS_CHANGE,
					ActivityEntityType.MISSION,
					mission.getId(),
					ActivityEntityType.metadata(
							"reference", mission.getReference(),
							"from", previousStatus.name(),
							"to", mission.getStatus().name()));
			return;
		}
		activityService.record(
				ActivityAction.UPDATE,
				ActivityEntityType.MISSION,
				mission.getId(),
				ActivityEntityType.metadata("reference", mission.getReference()));
	}

	private void transitionMission(Mission mission, MissionStatus target) {
		validateTransition(mission.getStatus(), target);
		applyStatusTimestamps(mission, target);
		mission.setStatus(target);
		if (target == MissionStatus.IN_PROGRESS) {
			startMissionResources(mission);
		}
		if (target == MissionStatus.COMPLETED || target == MissionStatus.CANCELLED) {
			releaseMissionResources(mission);
		}
	}

	private void validateTransition(MissionStatus current, MissionStatus target) {
		if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
			throw new ConflictException(
					"Invalid status transition: " + current + " -> " + target);
		}
	}

	private void startMissionResources(Mission mission) {
		if (mission.getDriver() == null || mission.getVehicle() == null) {
			throw new BusinessException("A mission requires an assigned driver and vehicle to start");
		}
		assertDriverAvailable(mission.getDriver());
		assertVehicleAvailable(mission.getVehicle());
		mission.getDriver().setStatus(DriverStatus.ON_MISSION);
		mission.getVehicle().setStatus(VehicleStatus.IN_USE);
	}

	private void releaseMissionResources(Mission mission) {
		Driver driver = mission.getDriver();
		if (driver != null && driver.getStatus() == DriverStatus.ON_MISSION) {
			driver.setStatus(DriverStatus.AVAILABLE);
		}
		Vehicle vehicle = mission.getVehicle();
		if (vehicle != null && vehicle.getStatus() == VehicleStatus.IN_USE) {
			vehicle.setStatus(VehicleStatus.AVAILABLE);
		}
	}

	private void assertNotStartedForReassignment(
			Mission mission, boolean updateDriver, boolean updateVehicle, Driver driver, Vehicle vehicle) {
		if (mission.getStatus() != MissionStatus.PLANIFIED) {
			if (updateDriver && driver != mission.getDriver()) {
				throw new ConflictException(
						"Driver can only be reassigned while the mission is planned: " + mission.getReference());
			}
			if (updateVehicle && vehicle != mission.getVehicle()) {
				throw new ConflictException(
						"Vehicle can only be reassigned while the mission is planned: " + mission.getReference());
			}
			return;
		}
		Driver effectiveDriver = updateDriver ? driver : mission.getDriver();
		Vehicle effectiveVehicle = updateVehicle ? vehicle : mission.getVehicle();
		if (effectiveDriver != null && effectiveDriver.getStatus() == DriverStatus.ON_MISSION) {
			throw new ConflictException("Driver is already on another mission: "
					+ effectiveDriver.getFirstName() + " " + effectiveDriver.getLastName());
		}
		if (effectiveVehicle != null && effectiveVehicle.getStatus() == VehicleStatus.IN_USE) {
			throw new ConflictException(
					"Vehicle is already in use on another mission: " + effectiveVehicle.getRegistrationNumber());
		}
	}

	private void assertDriverAvailable(Driver driver) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		if (driver.getStatus() != DriverStatus.AVAILABLE) {
			throw new BusinessException("Driver is not available: "
					+ driver.getFirstName() + " " + driver.getLastName());
		}
		if (driver.getLicenseExpiresAt().isBefore(today)) {
			throw new BusinessException("Driver license is expired: "
					+ driver.getFirstName() + " " + driver.getLastName());
		}
	}

	private void assertVehicleAvailable(Vehicle vehicle) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
			throw new BusinessException("Vehicle is not available: " + vehicle.getRegistrationNumber());
		}
		if (vehicle.getInsuranceExpiresAt() != null && vehicle.getInsuranceExpiresAt().isBefore(today)) {
			throw new BusinessException("Vehicle insurance is expired: " + vehicle.getRegistrationNumber());
		}
		if (vehicle.getTechnicalVisitExpiresAt() != null
				&& vehicle.getTechnicalVisitExpiresAt().isBefore(today)) {
			throw new BusinessException(
					"Vehicle technical visit is expired: " + vehicle.getRegistrationNumber());
		}
	}

	private void applyStatusTimestamps(Mission mission, MissionStatus status) {
		Instant now = Instant.now();
		if (status == MissionStatus.IN_PROGRESS && mission.getStartedAt() == null) {
			mission.setStartedAt(now);
		}
		if (status == MissionStatus.COMPLETED && mission.getCompletedAt() == null) {
			if (mission.getStartedAt() == null) {
				mission.setStartedAt(now);
			}
			mission.setCompletedAt(now);
		}
	}

	private void assertCanReadMission(Mission mission) {
		if (hasAnyRole("ADMIN", "TRANSPORT_MANAGER")) {
			return;
		}
		if (hasRole("DRIVER") && isAssignedToCurrentUser(mission)) {
			return;
		}
		throw new AccessDeniedException("Access denied");
	}

	private void assertCanManageMission(Mission mission) {
		if (hasAnyRole("ADMIN", "TRANSPORT_MANAGER")) {
			return;
		}
		if (hasRole("DRIVER") && isAssignedToCurrentUser(mission)) {
			return;
		}
		throw new AccessDeniedException("Access denied");
	}

	private boolean isAssignedToCurrentUser(Mission mission) {
		if (mission.getDriver() == null || mission.getDriver().getUser() == null) {
			return false;
		}
		return mission.getDriver().getUser().getId().equals(getCurrentUser().getId());
	}

	private UUID requireCurrentDriverId() {
		UserPrincipal currentUser = getCurrentUser();
		return driverRepository.findByUser_Id(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for current user"))
				.getId();
	}

	private UserPrincipal getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			throw new AccessDeniedException("Access denied");
		}
		return principal;
	}

	private boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		String expected = "ROLE_" + role;
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (expected.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAnyRole(String... roles) {
		for (String role : roles) {
			if (hasRole(role)) {
				return true;
			}
		}
		return false;
	}

	private Mission findMission(UUID id) {
		return missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));
	}

	private Client findClient(UUID clientId) {
		return clientRepository.findById(clientId)
				.orElseThrow(() -> new BusinessException("Client not found with id: " + clientId));
	}

	private Driver resolveDriver(UUID driverId) {
		if (driverId == null) {
			return null;
		}
		return driverRepository.findById(driverId)
				.orElseThrow(() -> new BusinessException("Driver not found with id: " + driverId));
	}

	private Vehicle resolveVehicle(UUID vehicleId) {
		if (vehicleId == null) {
			return null;
		}
		return vehicleRepository.findById(vehicleId)
				.orElseThrow(() -> new BusinessException("Vehicle not found with id: " + vehicleId));
	}

	private User resolveUser(UUID userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("User not found with id: " + userId));
	}

	private User findCurrentUserOrNull() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			return null;
		}
		return userRepository.findById(principal.getId()).orElse(null);
	}

	private String normalizeSearch(String search) {
		if (search == null) {
			return null;
		}
		String trimmed = search.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

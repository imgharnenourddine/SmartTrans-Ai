package com.smarttrans.ai.driver.service.impl;

import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.driver.dto.CreateDriverRequest;
import com.smarttrans.ai.driver.dto.DriverResponse;
import com.smarttrans.ai.driver.dto.UpdateDriverAvailabilityRequest;
import com.smarttrans.ai.driver.dto.UpdateDriverRequest;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.driver.mapper.DriverMapper;
import com.smarttrans.ai.driver.repository.DriverRepository;
import com.smarttrans.ai.driver.service.DriverService;
import com.smarttrans.ai.exception.BusinessException;
import com.smarttrans.ai.exception.ConflictException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.security.service.UserPrincipal;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
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
public class DriverServiceImpl implements DriverService {

	private static final Set<DriverStatus> AVAILABILITY_STATUSES = Set.of(
			DriverStatus.AVAILABLE, DriverStatus.UNAVAILABLE);

	private final DriverRepository driverRepository;
	private final UserRepository userRepository;
	private final DriverMapper driverMapper;
	private final ActivityService activityService;

	@Override
	public DriverResponse create(CreateDriverRequest request) {
		String licenseNumber = request.getLicenseNumber().trim();
		if (driverRepository.existsByLicenseNumber(licenseNumber)) {
			throw new ConflictException("License number already in use: " + licenseNumber);
		}

		User user = resolveUser(request.getUserId());
		if (user != null && driverRepository.existsByUser_Id(user.getId())) {
			throw new ConflictException("User already linked to a driver: " + user.getId());
		}

		Driver saved = driverRepository.save(driverMapper.toEntity(request, user));
		activityService.record(
				ActivityAction.CREATE,
				ActivityEntityType.DRIVER,
				saved.getId(),
				driverMetadata(saved));
		return driverMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public DriverResponse getById(UUID id) {
		Driver driver = findDriver(id);
		assertCanReadDriver(driver);
		return driverMapper.toResponse(driver);
	}

	@Override
	@Transactional(readOnly = true)
	public DriverResponse getMe() {
		UserPrincipal currentUser = getCurrentUser();
		Driver driver = driverRepository.findByUser_Id(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for current user"));
		return driverMapper.toResponse(driver);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DriverResponse> search(
			String search, DriverStatus status, Boolean licenseExpired, Boolean availableOnly) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		String normalizedSearch = normalizeSearch(search);
		return driverRepository.search(normalizedSearch, status, licenseExpired, availableOnly, today).stream()
				.map(driverMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DriverResponse> findExpiringLicenses(int withinDays) {
		if (withinDays < 0) {
			throw new BusinessException("withinDays must be zero or positive");
		}
		LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(withinDays);
		return driverRepository.findExpiringLicenses(deadline).stream()
				.map(driverMapper::toResponse)
				.toList();
	}

	@Override
	public DriverResponse update(UUID id, UpdateDriverRequest request) {
		Driver driver = findDriver(id);

		if (request.getLicenseNumber() != null) {
			String licenseNumber = request.getLicenseNumber().trim();
			if (!licenseNumber.equalsIgnoreCase(driver.getLicenseNumber())
					&& driverRepository.existsByLicenseNumber(licenseNumber)) {
				throw new ConflictException("License number already in use: " + licenseNumber);
			}
		}

		boolean updateUser = request.getUserId() != null;
		User user = null;
		if (updateUser) {
			user = resolveUser(request.getUserId());
			UUID currentUserId = driver.getUser() != null ? driver.getUser().getId() : null;
			if (user != null && !user.getId().equals(currentUserId) && driverRepository.existsByUser_Id(user.getId())) {
				throw new ConflictException("User already linked to a driver: " + user.getId());
			}
		}

		if (request.getStatus() == DriverStatus.SUSPENDED && driver.getStatus() == DriverStatus.ON_MISSION) {
			throw new ConflictException("Cannot suspend a driver currently on mission");
		}

		DriverStatus previousStatus = driver.getStatus();
		driverMapper.updateEntity(driver, request, user, updateUser);
		recordDriverChange(driver, previousStatus);
		return driverMapper.toResponse(driver);
	}

	@Override
	public DriverResponse updateAvailability(UUID id, UpdateDriverAvailabilityRequest request) {
		Driver driver = findDriver(id);
		assertCanManageAvailability(driver);

		DriverStatus targetStatus = request.getStatus();
		if (!AVAILABILITY_STATUSES.contains(targetStatus)) {
			throw new BusinessException("Availability can only be set to AVAILABLE or UNAVAILABLE");
		}

		if (driver.getStatus() == DriverStatus.ON_MISSION) {
			throw new ConflictException("Cannot change availability while driver is on mission");
		}

		if (driver.getStatus() == DriverStatus.SUSPENDED) {
			throw new ConflictException("Suspended driver availability cannot be changed from this endpoint");
		}

		if (targetStatus == DriverStatus.AVAILABLE && isLicenseExpired(driver)) {
			throw new BusinessException("Cannot set driver as available: driving license is expired");
		}

		DriverStatus previousStatus = driver.getStatus();
		driver.setStatus(targetStatus);
		recordDriverChange(driver, previousStatus);
		return driverMapper.toResponse(driver);
	}

	@Override
	public DriverResponse deactivate(UUID id) {
		Driver driver = findDriver(id);
		if (driver.getStatus() == DriverStatus.ON_MISSION) {
			throw new ConflictException("Cannot deactivate a driver currently on mission");
		}
		DriverStatus previousStatus = driver.getStatus();
		driver.setStatus(DriverStatus.SUSPENDED);
		recordDriverChange(driver, previousStatus);
		return driverMapper.toResponse(driver);
	}

	@Override
	public void delete(UUID id) {
		deactivate(id);
	}

	private void recordDriverChange(Driver driver, DriverStatus previousStatus) {
		if (driver.getStatus() != previousStatus) {
			activityService.record(
					ActivityAction.STATUS_CHANGE,
					ActivityEntityType.DRIVER,
					driver.getId(),
					ActivityEntityType.metadata(
							"firstName", driver.getFirstName(),
							"lastName", driver.getLastName(),
							"from", previousStatus.name(),
							"to", driver.getStatus().name()));
			return;
		}
		activityService.record(
				ActivityAction.UPDATE,
				ActivityEntityType.DRIVER,
				driver.getId(),
				driverMetadata(driver));
	}

	private Map<String, Object> driverMetadata(Driver driver) {
		return ActivityEntityType.metadata(
				"firstName", driver.getFirstName(),
				"lastName", driver.getLastName(),
				"licenseNumber", driver.getLicenseNumber());
	}

	private Driver findDriver(UUID id) {
		return driverRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Driver", id));
	}

	private User resolveUser(UUID userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("User not found with id: " + userId));
	}

	private void assertCanReadDriver(Driver driver) {
		if (hasAnyRole("ADMIN", "TRANSPORT_MANAGER")) {
			return;
		}
		if (hasRole("DRIVER") && isLinkedToCurrentUser(driver)) {
			return;
		}
		throw new AccessDeniedException("Access denied");
	}

	private void assertCanManageAvailability(Driver driver) {
		if (hasAnyRole("ADMIN", "TRANSPORT_MANAGER")) {
			return;
		}
		if (hasRole("DRIVER") && isLinkedToCurrentUser(driver)) {
			return;
		}
		throw new AccessDeniedException("Access denied");
	}

	private boolean isLinkedToCurrentUser(Driver driver) {
		if (driver.getUser() == null) {
			return false;
		}
		return driver.getUser().getId().equals(getCurrentUser().getId());
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

	private boolean isLicenseExpired(Driver driver) {
		return driver.getLicenseExpiresAt().isBefore(LocalDate.now(ZoneOffset.UTC));
	}

	private String normalizeSearch(String search) {
		if (search == null) {
			return null;
		}
		String trimmed = search.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

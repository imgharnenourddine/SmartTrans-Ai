package com.smarttrans.ai.notification.service.impl;

import com.smarttrans.ai.exception.BusinessException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.mission.repository.MissionRepository;
import com.smarttrans.ai.notification.dto.CreateNotificationRequest;
import com.smarttrans.ai.notification.dto.NotificationResponse;
import com.smarttrans.ai.notification.dto.UpdateNotificationRequest;
import com.smarttrans.ai.notification.entity.Notification;
import com.smarttrans.ai.notification.mapper.NotificationMapper;
import com.smarttrans.ai.notification.repository.NotificationRepository;
import com.smarttrans.ai.notification.service.NotificationService;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.repository.VehicleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final MissionRepository missionRepository;
	private final VehicleRepository vehicleRepository;
	private final NotificationMapper notificationMapper;

	@Override
	public NotificationResponse create(CreateNotificationRequest request) {
		User user = findUser(request.getUserId());
		Mission mission = resolveMission(request.getMissionId());
		Vehicle vehicle = resolveVehicle(request.getVehicleId());
		Notification saved = notificationRepository.save(notificationMapper.toEntity(request, user, mission, vehicle));
		return notificationMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public NotificationResponse getById(UUID id) {
		return notificationMapper.toResponse(findNotification(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationResponse> getAll() {
		return notificationRepository.findAll().stream().map(notificationMapper::toResponse).toList();
	}

	@Override
	public NotificationResponse update(UUID id, UpdateNotificationRequest request) {
		Notification notification = findNotification(id);

		boolean updateMission = request.getMissionId() != null;
		Mission mission = updateMission ? resolveMission(request.getMissionId()) : null;

		boolean updateVehicle = request.getVehicleId() != null;
		Vehicle vehicle = updateVehicle ? resolveVehicle(request.getVehicleId()) : null;

		notificationMapper.updateEntity(notification, request, mission, updateMission, vehicle, updateVehicle);
		return notificationMapper.toResponse(notification);
	}

	@Override
	public void delete(UUID id) {
		if (!notificationRepository.existsById(id)) {
			throw new ResourceNotFoundException("Notification", id);
		}
		notificationRepository.deleteById(id);
	}

	private Notification findNotification(UUID id) {
		return notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification", id));
	}

	private User findUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("User not found with id: " + userId));
	}

	private Mission resolveMission(UUID missionId) {
		if (missionId == null) {
			return null;
		}
		return missionRepository.findById(missionId)
				.orElseThrow(() -> new BusinessException("Mission not found with id: " + missionId));
	}

	private Vehicle resolveVehicle(UUID vehicleId) {
		if (vehicleId == null) {
			return null;
		}
		return vehicleRepository.findById(vehicleId)
				.orElseThrow(() -> new BusinessException("Vehicle not found with id: " + vehicleId));
	}
}

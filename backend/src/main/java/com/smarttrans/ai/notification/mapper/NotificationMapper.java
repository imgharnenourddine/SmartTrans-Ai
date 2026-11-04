package com.smarttrans.ai.notification.mapper;

import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.notification.dto.CreateNotificationRequest;
import com.smarttrans.ai.notification.dto.NotificationResponse;
import com.smarttrans.ai.notification.dto.UpdateNotificationRequest;
import com.smarttrans.ai.notification.entity.Notification;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

	public Notification toEntity(
			CreateNotificationRequest request,
			User user,
			Mission mission,
			Vehicle vehicle) {
		return Notification.builder()
				.user(user)
				.type(request.getType())
				.title(request.getTitle().trim())
				.body(request.getBody().trim())
				.mission(mission)
				.vehicle(vehicle)
				.build();
	}

	public void updateEntity(
			Notification notification,
			UpdateNotificationRequest request,
			Mission mission,
			boolean updateMission,
			Vehicle vehicle,
			boolean updateVehicle) {
		if (request.getType() != null) {
			notification.setType(request.getType());
		}
		if (request.getTitle() != null) {
			notification.setTitle(request.getTitle().trim());
		}
		if (request.getBody() != null) {
			notification.setBody(request.getBody().trim());
		}
		if (updateMission) {
			notification.setMission(mission);
		}
		if (updateVehicle) {
			notification.setVehicle(vehicle);
		}
		if (Boolean.TRUE.equals(request.getMarkAsRead()) && notification.getReadAt() == null) {
			notification.setReadAt(Instant.now());
		} else if (request.getReadAt() != null) {
			notification.setReadAt(request.getReadAt());
		}
	}

	public NotificationResponse toResponse(Notification notification) {
		return NotificationResponse.builder()
				.id(notification.getId())
				.userId(notification.getUser() != null ? notification.getUser().getId() : null)
				.type(notification.getType())
				.title(notification.getTitle())
				.body(notification.getBody())
				.missionId(notification.getMission() != null ? notification.getMission().getId() : null)
				.vehicleId(notification.getVehicle() != null ? notification.getVehicle().getId() : null)
				.readAt(notification.getReadAt())
				.createdAt(notification.getCreatedAt())
				.build();
	}
}

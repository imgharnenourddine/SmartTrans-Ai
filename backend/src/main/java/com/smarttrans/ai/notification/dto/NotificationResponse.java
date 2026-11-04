package com.smarttrans.ai.notification.dto;

import com.smarttrans.ai.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

	private UUID id;
	private UUID userId;
	private NotificationType type;
	private String title;
	private String body;
	private UUID missionId;
	private UUID vehicleId;
	private Instant readAt;
	private Instant createdAt;
}

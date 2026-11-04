package com.smarttrans.ai.notification.dto;

import com.smarttrans.ai.notification.enums.NotificationType;
import jakarta.validation.constraints.Size;
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
public class UpdateNotificationRequest {

	private NotificationType type;

	@Size(max = 255)
	private String title;

	private String body;

	private UUID missionId;

	private UUID vehicleId;

	private Instant readAt;

	private Boolean markAsRead;
}

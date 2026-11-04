package com.smarttrans.ai.notification.dto;

import com.smarttrans.ai.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateNotificationRequest {

	@NotNull
	private UUID userId;

	@NotNull
	private NotificationType type;

	@NotBlank
	@Size(max = 255)
	private String title;

	@NotBlank
	private String body;

	private UUID missionId;

	private UUID vehicleId;
}

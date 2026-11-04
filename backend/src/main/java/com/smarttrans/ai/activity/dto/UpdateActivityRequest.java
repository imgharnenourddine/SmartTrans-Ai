package com.smarttrans.ai.activity.dto;

import com.smarttrans.ai.activity.enums.ActivityAction;
import jakarta.validation.constraints.Size;
import java.util.Map;
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
public class UpdateActivityRequest {

	private UUID actorUserId;

	private ActivityAction action;

	@Size(max = 50)
	private String entityType;

	private UUID entityId;

	private Map<String, Object> metadata;
}

package com.smarttrans.ai.activity.dto;

import com.smarttrans.ai.activity.enums.ActivityAction;
import java.time.Instant;
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
public class ActivityResponse {

	private UUID id;
	private UUID actorUserId;
	private ActivityAction action;
	private String entityType;
	private UUID entityId;
	private Map<String, Object> metadata;
	private Instant createdAt;
}

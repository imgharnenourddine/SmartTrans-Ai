package com.smarttrans.ai.role.dto;

import com.smarttrans.ai.common.enums.RoleCode;
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
public class RoleResponse {

	private UUID id;
	private RoleCode code;
	private String label;
	private Instant createdAt;
}

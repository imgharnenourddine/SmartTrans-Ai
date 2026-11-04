package com.smarttrans.ai.user.dto;

import com.smarttrans.ai.common.enums.RoleCode;
import java.time.Instant;
import java.util.Set;
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
public class UserResponse {

	private UUID id;
	private String email;
	private String firstName;
	private String lastName;
	private String phone;
	private boolean enabled;
	private Instant lastLoginAt;
	private Set<RoleCode> roleCodes;
	private Instant createdAt;
	private Instant updatedAt;
}

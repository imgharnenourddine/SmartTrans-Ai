package com.smarttrans.ai.auth.dto;

import com.smarttrans.ai.common.enums.RoleCode;
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
public class LoginResponse {

	private String accessToken;
	private String tokenType;
	private long expiresInMs;
	private UUID userId;
	private String email;
	private String firstName;
	private String lastName;
	private Set<RoleCode> roleCodes;
}

package com.smarttrans.ai.user.dto;

import com.smarttrans.ai.common.enums.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Set;
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
public class UpdateUserRequest {

	@Email
	@Size(max = 255)
	private String email;

	@Size(max = 100)
	private String firstName;

	@Size(max = 100)
	private String lastName;

	@Size(max = 30)
	private String phone;

	private Boolean enabled;

	private Set<RoleCode> roleCodes;
}

package com.smarttrans.ai.role.dto;

import com.smarttrans.ai.common.enums.RoleCode;
import jakarta.validation.constraints.Size;
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
public class UpdateRoleRequest {

	private RoleCode code;

	@Size(max = 100)
	private String label;
}

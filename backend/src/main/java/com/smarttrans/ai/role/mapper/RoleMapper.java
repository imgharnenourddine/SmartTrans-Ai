package com.smarttrans.ai.role.mapper;

import com.smarttrans.ai.role.dto.CreateRoleRequest;
import com.smarttrans.ai.role.dto.RoleResponse;
import com.smarttrans.ai.role.dto.UpdateRoleRequest;
import com.smarttrans.ai.role.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

	public Role toEntity(CreateRoleRequest request) {
		return Role.builder()
				.code(request.getCode())
				.label(request.getLabel().trim())
				.build();
	}

	public void updateEntity(Role role, UpdateRoleRequest request) {
		if (request.getCode() != null) {
			role.setCode(request.getCode());
		}
		if (request.getLabel() != null) {
			role.setLabel(request.getLabel().trim());
		}
	}

	public RoleResponse toResponse(Role role) {
		return RoleResponse.builder()
				.id(role.getId())
				.code(role.getCode())
				.label(role.getLabel())
				.createdAt(role.getCreatedAt())
				.build();
	}
}

package com.smarttrans.ai.user.mapper;

import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.user.dto.CreateUserRequest;
import com.smarttrans.ai.user.dto.UpdateUserRequest;
import com.smarttrans.ai.user.dto.UserResponse;
import com.smarttrans.ai.user.entity.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public User toEntity(CreateUserRequest request, String passwordHash, Set<Role> roles) {
		return User.builder()
				.email(request.getEmail().trim().toLowerCase())
				.passwordHash(passwordHash)
				.firstName(request.getFirstName().trim())
				.lastName(request.getLastName().trim())
				.phone(normalizePhone(request.getPhone()))
				.enabled(true)
				.roles(roles)
				.build();
	}

	public void updateEntity(User user, UpdateUserRequest request, Set<Role> roles) {
		if (request.getEmail() != null) {
			user.setEmail(request.getEmail().trim().toLowerCase());
		}
		if (request.getFirstName() != null) {
			user.setFirstName(request.getFirstName().trim());
		}
		if (request.getLastName() != null) {
			user.setLastName(request.getLastName().trim());
		}
		if (request.getPhone() != null) {
			user.setPhone(normalizePhone(request.getPhone()));
		}
		if (request.getEnabled() != null) {
			user.setEnabled(request.getEnabled());
		}
		if (roles != null) {
			user.setRoles(roles);
		}
	}

	public UserResponse toResponse(User user) {
		Set<RoleCode> roleCodes = user.getRoles().stream()
				.map(Role::getCode)
				.collect(Collectors.toSet());

		return UserResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.phone(user.getPhone())
				.enabled(user.isEnabled())
				.lastLoginAt(user.getLastLoginAt())
				.roleCodes(roleCodes)
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.build();
	}

	private String normalizePhone(String phone) {
		if (phone == null) {
			return null;
		}
		String trimmed = phone.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

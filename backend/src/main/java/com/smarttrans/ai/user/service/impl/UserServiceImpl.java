package com.smarttrans.ai.user.service.impl;

import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.exception.BusinessException;
import com.smarttrans.ai.exception.ConflictException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.role.repository.RoleRepository;
import com.smarttrans.ai.user.dto.CreateUserRequest;
import com.smarttrans.ai.user.dto.UpdateUserRequest;
import com.smarttrans.ai.user.dto.UserResponse;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.mapper.UserMapper;
import com.smarttrans.ai.user.repository.UserRepository;
import com.smarttrans.ai.user.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final ActivityService activityService;

	@Override
	public UserResponse create(CreateUserRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already in use: " + email);
		}

		Set<Role> roles = resolveRoles(request.getRoleCodes());
		String passwordHash = passwordEncoder.encode(request.getPassword());
		User user = userMapper.toEntity(request, passwordHash, roles);
		User saved = userRepository.save(user);
		activityService.record(
				ActivityAction.CREATE,
				ActivityEntityType.USER,
				saved.getId(),
				ActivityEntityType.metadata("email", saved.getEmail()));
		return userMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getById(UUID id) {
		User user = userRepository.findByIdWithRoles(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
		return userMapper.toResponse(user);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponse> getAll() {
		return userRepository.findAllWithRoles().stream()
				.map(userMapper::toResponse)
				.toList();
	}

	@Override
	public UserResponse update(UUID id, UpdateUserRequest request) {
		User user = userRepository.findByIdWithRoles(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));

		if (request.getEmail() != null) {
			String email = request.getEmail().trim().toLowerCase();
			if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
				throw new ConflictException("Email already in use: " + email);
			}
		}

		Set<Role> roles = request.getRoleCodes() != null ? resolveRoles(request.getRoleCodes()) : null;
		userMapper.updateEntity(user, request, roles);
		activityService.record(
				ActivityAction.UPDATE,
				ActivityEntityType.USER,
				user.getId(),
				ActivityEntityType.metadata("email", user.getEmail()));
		return userMapper.toResponse(user);
	}

	@Override
	public void delete(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
		activityService.record(
				ActivityAction.DELETE,
				ActivityEntityType.USER,
				user.getId(),
				ActivityEntityType.metadata("email", user.getEmail()));
		userRepository.delete(user);
	}

	private Set<Role> resolveRoles(Set<RoleCode> roleCodes) {
		if (roleCodes == null || roleCodes.isEmpty()) {
			throw new BusinessException("At least one role is required");
		}

		List<Role> roles = roleRepository.findByCodeIn(roleCodes);
		if (roles.size() != roleCodes.size()) {
			Set<RoleCode> found = roles.stream().map(Role::getCode).collect(Collectors.toSet());
			Set<RoleCode> missing = roleCodes.stream()
					.filter(code -> !found.contains(code))
					.collect(Collectors.toSet());
			throw new BusinessException("Unknown role code(s): " + missing);
		}
		return new HashSet<>(roles);
	}
}

package com.smarttrans.ai.role.service.impl;

import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.exception.ConflictException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.role.dto.CreateRoleRequest;
import com.smarttrans.ai.role.dto.RoleResponse;
import com.smarttrans.ai.role.dto.UpdateRoleRequest;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.role.mapper.RoleMapper;
import com.smarttrans.ai.role.repository.RoleRepository;
import com.smarttrans.ai.role.service.RoleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

	private final RoleRepository roleRepository;
	private final RoleMapper roleMapper;

	@Override
	public RoleResponse create(CreateRoleRequest request) {
		if (roleRepository.existsByCode(request.getCode())) {
			throw new ConflictException("Role code already exists: " + request.getCode());
		}
		Role saved = roleRepository.save(roleMapper.toEntity(request));
		return roleMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public RoleResponse getById(UUID id) {
		return roleMapper.toResponse(findRole(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoleResponse> getAll() {
		return roleRepository.findAll().stream().map(roleMapper::toResponse).toList();
	}

	@Override
	public RoleResponse update(UUID id, UpdateRoleRequest request) {
		Role role = findRole(id);
		if (request.getCode() != null && request.getCode() != role.getCode()) {
			ensureCodeAvailable(request.getCode());
		}
		roleMapper.updateEntity(role, request);
		return roleMapper.toResponse(role);
	}

	@Override
	public void delete(UUID id) {
		if (!roleRepository.existsById(id)) {
			throw new ResourceNotFoundException("Role", id);
		}
		roleRepository.deleteById(id);
	}

	private Role findRole(UUID id) {
		return roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role", id));
	}

	private void ensureCodeAvailable(RoleCode code) {
		if (roleRepository.existsByCode(code)) {
			throw new ConflictException("Role code already exists: " + code);
		}
	}
}

package com.smarttrans.ai.role.service;

import com.smarttrans.ai.role.dto.CreateRoleRequest;
import com.smarttrans.ai.role.dto.RoleResponse;
import com.smarttrans.ai.role.dto.UpdateRoleRequest;
import java.util.List;
import java.util.UUID;

public interface RoleService {

	RoleResponse create(CreateRoleRequest request);

	RoleResponse getById(UUID id);

	List<RoleResponse> getAll();

	RoleResponse update(UUID id, UpdateRoleRequest request);

	void delete(UUID id);
}

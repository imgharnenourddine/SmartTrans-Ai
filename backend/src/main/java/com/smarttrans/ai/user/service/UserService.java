package com.smarttrans.ai.user.service;

import com.smarttrans.ai.user.dto.CreateUserRequest;
import com.smarttrans.ai.user.dto.UpdateUserRequest;
import com.smarttrans.ai.user.dto.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {

	UserResponse create(CreateUserRequest request);

	UserResponse getById(UUID id);

	List<UserResponse> getAll();

	UserResponse update(UUID id, UpdateUserRequest request);

	void delete(UUID id);
}

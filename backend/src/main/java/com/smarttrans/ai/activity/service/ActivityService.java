package com.smarttrans.ai.activity.service;

import com.smarttrans.ai.activity.dto.ActivityResponse;
import com.smarttrans.ai.activity.dto.CreateActivityRequest;
import com.smarttrans.ai.activity.dto.UpdateActivityRequest;
import com.smarttrans.ai.activity.enums.ActivityAction;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ActivityService {

	void record(ActivityAction action, String entityType, UUID entityId, Map<String, Object> metadata);

	void record(UUID actorUserId, ActivityAction action, String entityType, UUID entityId, Map<String, Object> metadata);

	ActivityResponse create(CreateActivityRequest request);

	ActivityResponse getById(UUID id);

	List<ActivityResponse> getAll();

	ActivityResponse update(UUID id, UpdateActivityRequest request);

	void delete(UUID id);
}

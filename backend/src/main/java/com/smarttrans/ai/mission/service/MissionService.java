package com.smarttrans.ai.mission.service;

import com.smarttrans.ai.mission.dto.CreateMissionRequest;
import com.smarttrans.ai.mission.dto.MissionResponse;
import com.smarttrans.ai.mission.dto.MissionStatusUpdateRequest;
import com.smarttrans.ai.mission.dto.UpdateMissionRequest;
import com.smarttrans.ai.mission.enums.MissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MissionService {

	MissionResponse create(CreateMissionRequest request);

	MissionResponse getById(UUID id);

	List<MissionResponse> search(String search, MissionStatus status, UUID driverId, UUID vehicleId,
			UUID clientId, Instant scheduledFrom, Instant scheduledTo);

	List<MissionResponse> getMyMissions();

	MissionResponse update(UUID id, UpdateMissionRequest request);

	MissionResponse updateStatus(UUID id, MissionStatusUpdateRequest request);

	void delete(UUID id);
}

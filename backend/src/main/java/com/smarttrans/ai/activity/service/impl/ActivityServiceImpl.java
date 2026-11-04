package com.smarttrans.ai.activity.service.impl;

import com.smarttrans.ai.activity.dto.ActivityResponse;
import com.smarttrans.ai.activity.dto.CreateActivityRequest;
import com.smarttrans.ai.activity.dto.UpdateActivityRequest;
import com.smarttrans.ai.activity.entity.Activity;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.mapper.ActivityMapper;
import com.smarttrans.ai.activity.repository.ActivityRepository;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.exception.BusinessException;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import com.smarttrans.ai.security.service.UserPrincipal;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityServiceImpl implements ActivityService {

	private final ActivityRepository activityRepository;
	private final UserRepository userRepository;
	private final ActivityMapper activityMapper;

	@Override
	public void record(ActivityAction action, String entityType, UUID entityId, Map<String, Object> metadata) {
		record(currentActorIdOrNull(), action, entityType, entityId, metadata);
	}

	@Override
	public void record(UUID actorUserId, ActivityAction action, String entityType, UUID entityId,
			Map<String, Object> metadata) {
		User actor = resolveUser(actorUserId);
		activityRepository.save(Activity.builder()
				.actor(actor)
				.action(action)
				.entityType(entityType)
				.entityId(entityId)
				.metadata(metadata)
				.build());
	}

	@Override
	public ActivityResponse create(CreateActivityRequest request) {
		User actor = resolveUser(request.getActorUserId());
		Activity saved = activityRepository.save(activityMapper.toEntity(request, actor));
		return activityMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public ActivityResponse getById(UUID id) {
		return activityMapper.toResponse(findActivity(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ActivityResponse> getAll() {
		return activityRepository.findAllByOrderByCreatedAtDesc().stream().map(activityMapper::toResponse).toList();
	}

	@Override
	public ActivityResponse update(UUID id, UpdateActivityRequest request) {
		Activity activity = findActivity(id);
		boolean updateActor = request.getActorUserId() != null;
		User actor = updateActor ? resolveUser(request.getActorUserId()) : null;
		activityMapper.updateEntity(activity, request, actor, updateActor);
		return activityMapper.toResponse(activity);
	}

	@Override
	public void delete(UUID id) {
		if (!activityRepository.existsById(id)) {
			throw new ResourceNotFoundException("Activity", id);
		}
		activityRepository.deleteById(id);
	}

	private Activity findActivity(UUID id) {
		return activityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Activity", id));
	}

	private UUID currentActorIdOrNull() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			return null;
		}
		return principal.getId();
	}

	private User resolveUser(UUID userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("User not found with id: " + userId));
	}
}

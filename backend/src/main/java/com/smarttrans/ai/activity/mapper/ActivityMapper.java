package com.smarttrans.ai.activity.mapper;

import com.smarttrans.ai.activity.dto.ActivityResponse;
import com.smarttrans.ai.activity.dto.CreateActivityRequest;
import com.smarttrans.ai.activity.dto.UpdateActivityRequest;
import com.smarttrans.ai.activity.entity.Activity;
import com.smarttrans.ai.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {

	public Activity toEntity(CreateActivityRequest request, User actor) {
		return Activity.builder()
				.actor(actor)
				.action(request.getAction())
				.entityType(request.getEntityType().trim())
				.entityId(request.getEntityId())
				.metadata(request.getMetadata())
				.build();
	}

	public void updateEntity(Activity activity, UpdateActivityRequest request, User actor, boolean updateActor) {
		if (updateActor) {
			activity.setActor(actor);
		}
		if (request.getAction() != null) {
			activity.setAction(request.getAction());
		}
		if (request.getEntityType() != null) {
			activity.setEntityType(request.getEntityType().trim());
		}
		if (request.getEntityId() != null) {
			activity.setEntityId(request.getEntityId());
		}
		if (request.getMetadata() != null) {
			activity.setMetadata(request.getMetadata());
		}
	}

	public ActivityResponse toResponse(Activity activity) {
		return ActivityResponse.builder()
				.id(activity.getId())
				.actorUserId(activity.getActor() != null ? activity.getActor().getId() : null)
				.action(activity.getAction())
				.entityType(activity.getEntityType())
				.entityId(activity.getEntityId())
				.metadata(activity.getMetadata())
				.createdAt(activity.getCreatedAt())
				.build();
	}
}

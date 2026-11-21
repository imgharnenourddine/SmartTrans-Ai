package com.smarttrans.ai.activity;

import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Activity;
import com.smarttrans.ai.domain.ActivityAction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class ActivityService {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    public ActivityResponse getById(UUID id) {
        Activity activity = entityManager.find(Activity.class, id);
        if (activity == null) {
            throw new NotFound("Activity not found with id: " + id);
        }
        return toResponse(activity);
    }

    public List<ActivityResponse> getAll() {
        return entityManager.createQuery(
                        "select a from Activity a left join fetch a.actor order by a.createdAt desc",
                        Activity.class)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getActor() != null ? activity.getActor().getId() : null,
                activity.getAction(),
                activity.getEntityType(),
                activity.getEntityId(),
                activity.getMetadata(),
                activity.getCreatedAt());
    }

    public record ActivityResponse(
            UUID id,
            UUID actorUserId,
            ActivityAction action,
            String entityType,
            UUID entityId,
            Map<String, Object> metadata,
            Instant createdAt) {
    }
}

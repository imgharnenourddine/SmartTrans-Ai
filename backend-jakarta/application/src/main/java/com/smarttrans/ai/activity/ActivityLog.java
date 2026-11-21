package com.smarttrans.ai.activity;

import com.smarttrans.ai.domain.Activity;
import com.smarttrans.ai.domain.ActivityAction;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class ActivityLog {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Inject
    private CurrentUser currentUser;

    public void record(ActivityAction action, String entityType, java.util.UUID entityId, Map<String, Object> metadata) {
        Activity activity = new Activity();
        if (currentUser.isAuthenticated()) {
            activity.setActor(entityManager.getReference(UserAccount.class, currentUser.id()));
        }
        activity.setAction(action);
        activity.setEntityType(entityType);
        activity.setEntityId(entityId);
        activity.setMetadata(metadata == null || metadata.isEmpty() ? null : metadata);
        entityManager.persist(activity);
    }

    public static Map<String, Object> metadata(Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null) {
                metadata.put(String.valueOf(keyValues[i]), value);
            }
        }
        return metadata.isEmpty() ? null : metadata;
    }
}

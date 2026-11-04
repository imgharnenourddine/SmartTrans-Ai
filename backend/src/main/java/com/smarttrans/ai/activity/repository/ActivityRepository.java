package com.smarttrans.ai.activity.repository;

import com.smarttrans.ai.activity.entity.Activity;
import com.smarttrans.ai.activity.enums.ActivityAction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

	List<Activity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

	List<Activity> findByActor_IdOrderByCreatedAtDesc(UUID actorId);

	List<Activity> findByActionOrderByCreatedAtDesc(ActivityAction action);

	List<Activity> findAllByOrderByCreatedAtDesc();
}

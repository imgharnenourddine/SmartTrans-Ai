package com.smarttrans.ai.notification.repository;

import com.smarttrans.ai.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);

	List<Notification> findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId);

	long countByUser_IdAndReadAtIsNull(UUID userId);
}

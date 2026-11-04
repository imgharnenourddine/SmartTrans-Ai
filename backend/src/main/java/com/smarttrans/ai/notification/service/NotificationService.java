package com.smarttrans.ai.notification.service;

import com.smarttrans.ai.notification.dto.CreateNotificationRequest;
import com.smarttrans.ai.notification.dto.NotificationResponse;
import com.smarttrans.ai.notification.dto.UpdateNotificationRequest;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

	NotificationResponse create(CreateNotificationRequest request);

	NotificationResponse getById(UUID id);

	List<NotificationResponse> getAll();

	NotificationResponse update(UUID id, UpdateNotificationRequest request);

	void delete(UUID id);
}

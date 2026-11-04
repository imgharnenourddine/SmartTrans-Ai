package com.smarttrans.ai.assistant.service;

import com.smarttrans.ai.assistant.dto.AssistantAskRequest;
import com.smarttrans.ai.assistant.dto.AssistantAskResponse;

public interface AssistantService {

	AssistantAskResponse ask(AssistantAskRequest request);
}

package com.smarttrans.ai.client.service;

import com.smarttrans.ai.client.dto.ClientResponse;
import com.smarttrans.ai.client.dto.CreateClientRequest;
import com.smarttrans.ai.client.dto.UpdateClientRequest;
import java.util.List;
import java.util.UUID;

public interface ClientService {

	ClientResponse create(CreateClientRequest request);

	ClientResponse getById(UUID id);

	List<ClientResponse> getAll();

	ClientResponse update(UUID id, UpdateClientRequest request);

	void delete(UUID id);
}
